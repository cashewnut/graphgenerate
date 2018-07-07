package edu.fdu.se.graphgenerate;

import edu.fdu.se.graphgenerate.enums.EnumRelationShipType;
import edu.fdu.se.graphgenerate.enums.EnumVertexLabelType;
import edu.fdu.se.graphgenerate.model.*;
import edu.fdu.se.graphgenerate.service.IVertexService;
import edu.fdu.se.graphgenerate.service.impl.VertexService;
import edu.fdu.se.graphgenerate.utils.ConvertEnumUtil;
import edu.fdu.se.graphgenerate.utils.FileUtil;
import edu.fdu.se.graphgenerate.utils.LoadProperties;
import edu.fdu.se.graphgenerate.utils.Relation;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.ReferenceType;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Generate {

    private String projectName;

    private String pgName;

    private String className;

    private IVertexService service = new VertexService();

    private Map<String, Vertex> pgIdPair = new HashMap<>();

    private Map<String, Vertex> mdNIdPair = new HashMap<>();// map<method declaration name,vertex>

    private List<String> extendsClass;

    private List<String> implInterface;

    private Set<String> imports = new HashSet<>();

    private String packageClass = "";

    private Set<MethodCall> methodCall = new HashSet<>();

    private Map<String, Set<String>> classMethodPair = new HashMap<>();//类名:方法名集合

    private Map<String, String> inherit = new HashMap<>();//类的继承关系

    private Map<String, Set<String>> listenerClassMethodPair = new HashMap<>();  //<className,Set(className.methodName)> 只关注了内部类

    private Map<String, Vertex> listenerMethodVertexPair = new HashMap<>();  //<className.methodName,vertex> 只关注内部类

    public Generate() {
    }

    public Generate(String projectName) {
        this.projectName = projectName;
        extendsClass = new ArrayList<>();
        implInterface = new ArrayList<>();
    }

    public void analyzePJ(File file) {
        System.out.println(">>>>>>>>>>>>>>>>>>>> analyze " + projectName + ">>>>>>>>>>>>>>>>>>>>");
        Vertex v = new Vertex();
        v.addLabel(projectName);
        v.addLabel(EnumVertexLabelType.PROJECT.getValue());
        v.setName(projectName);
        service.saveVertex(v);
        System.out.println(projectName + "'s basepath is " + file.getAbsolutePath());
        List<File> javaFiles = FileUtil.getJavaFiles(file);
        System.out.println(projectName + " has " + javaFiles.size() + " class!");

        //分析各个java文件中类继承关系和类与方法的对应关系
        for (File javaFile : javaFiles) {
            CompilationUnit cu = FileUtil.openCUByFile(javaFile.getAbsolutePath());
            ClassOrInterfaceDeclaration clazz = getClass(cu);

            if (clazz == null)
                continue;
            String name = clazz.getName();

            //分析继承结构
            if (clazz.getExtends() != null) {
                Set<String> except = new HashSet<>(Arrays.asList(LoadProperties.get("EXCEPTCLASS").split(",")));
                for (String ext : clazz.getExtends().stream().map(ClassOrInterfaceType::getName).collect(Collectors.toList())) {
                    if (!except.contains(ext)) {
                        inherit.put(name, ext);
                        break;
                    }
                }
            }
            if (!inherit.containsKey(name) && clazz.getImplements() != null) {
                Set<String> except = new HashSet<>(Arrays.asList(LoadProperties.get("EXCEPTCLASS").split(",")));
                for (String ext : clazz.getImplements().stream().map(ClassOrInterfaceType::getName).collect(Collectors.toList())) {
                    if (!except.contains(ext)) {
                        inherit.put(name, ext);
                        break;
                    }
                }
            }

            //分析类与方法的对应关系
            List<BodyDeclaration> body = clazz.getMembers();//获取一个类中的成员
            List<MethodDeclaration> methods = getMethodList(body);// method
            classMethodPair.put(name, methods.stream().map(MethodDeclaration::getName).collect(Collectors.toSet()));

            //统计listener的方法,用来填充listenerClassMethodPair
            List<ClassOrInterfaceDeclaration> innerClass = getInnerClass(body);// innerclass
            if (innerClass != null) {
                for (ClassOrInterfaceDeclaration inner : innerClass) {
                    try {
                        String implName = inner.getImplements().stream().map(ClassOrInterfaceType::getName).findFirst().get();
                        if (Pattern.matches(".*On.*Listener.*", implName)) {
                            String innerClassName = inner.getName();
                            List<MethodDeclaration> methodsOfInnerClass = getMethodList(inner.getMembers());
                            Set<String> innerClassMethod = methodsOfInnerClass.stream().map((n) -> (innerClassName + "." + n.getName())).collect(Collectors.toSet());
                            listenerClassMethodPair.put(innerClassName, innerClassMethod);
                        }
                    } catch (NullPointerException e) {

                    }
                }
            }
        }

        analyzeJavaFile(v, Objects.requireNonNull(javaFiles));
        callChain();

    }

    /*public void createProject(String projectPath) {
        Vertex v = new Vertex();
        v.addLabel(projectName);
        v.setName(projectName);
        service.saveVertex(v);
        List<String> javaFiles = FileUtil.javaFilePaths(projectPath);
        analyzeJavaFile(v, javaFiles);
    }*/

    /*private void analyzeJavaFile(Vertex pjVertex, @NotNull List<String> javaFiles) {
        for (String javaFile : javaFiles) {
            CompilationUnit cu = FileUtil.openCU(javaFile);
            Vertex pg = createPackage(pjVertex, cu);
            ClassOrInterfaceDeclaration clazz = getClass(cu);
            if (clazz == null)
                continue;
            Vertex clazzVertex = createClass(clazz);
            pgName = pg.getName();
            className = clazzVertex.getName();
            service.saveEdge(pg.getId(), clazzVertex.getId(), Relation.PARNET);
            List<BodyDeclaration> body = clazz.getMembers();//获取一个类中的成员
            Map<String, String> fieldMap = getField(body);

            List<MethodDeclaration> methods = getMethodList(body);// method
            for (MethodDeclaration method : methods) {
                Map<String, String> methodFieldMap = new HashMap<>(fieldMap);
                Vertex methodVertex = createMethod(method, methodFieldMap);
                service.saveEdge(clazzVertex.getId(), methodVertex.getId(), Relation.PARNET);
            }


        }
    }*/

    private void analyzeJavaFile(Vertex pjVertex, List<File> javaFiles) {
        for (File javaFile : javaFiles) {
            System.out.println("analyze javafile >>>>>>>>>>>>>>>>>> " + javaFile.getAbsolutePath());
            CompilationUnit cu = FileUtil.openCUByFile(javaFile.getAbsolutePath());
            Vertex pg = createPackage(pjVertex, cu);
            ClassOrInterfaceDeclaration clazz = getClass(cu);

            extendsClass = new ArrayList<>();
            implInterface = new ArrayList<>();

            if (clazz == null)
                continue;

            this.className = clazz.getName();

            if (clazz.getExtends() != null)
                extendsClass = clazz.getExtends().stream().map(ClassOrInterfaceType::getName).collect(Collectors.toList());

            if (clazz.getImplements() != null)
                implInterface = clazz.getImplements().stream().map(ClassOrInterfaceType::getName).collect(Collectors.toList());

            pgName = pg.getName();
            imports.clear();
            if (cu.getImports() != null && !cu.getImports().isEmpty())
                imports = cu.getImports().stream().map((n) -> n.getName().toString()).collect(Collectors.toSet());
            Vertex clazzVertex = createClass(clazz);
            service.saveEdge(pg.getId(), clazzVertex.getId(), Relation.PARNET);
            packageClass = cu.getPackage().getName().toString() + "." + cu.getTypes().get(0).getName();
            List<BodyDeclaration> body = clazz.getMembers();//获取一个类中的成员
            Map<String, String> fieldMap = getField(body);

            //分析Handler和Thread，Runnable
            List<FieldDeclaration> fds = getThreadOrHandlerField(body);
            for (FieldDeclaration fd : fds) {
                String type = ((ClassOrInterfaceType) ((ReferenceType) (fd.getType())).getType()).getName();
                Vertex threadClass = new Vertex();
                threadClass.addLabel(projectName);
                threadClass.addLabel(EnumVertexLabelType.CLASSORINTERFACE.getValue());
                threadClass.setCorrespondingPackage(pgName);
                threadClass.setCorrespondingClass(className);
                threadClass.setName(type);
                service.saveVertex(threadClass);
                service.saveEdge(clazzVertex.getId(), threadClass.getId(), Relation.PARNET);
                ObjectCreationExpr oc = (ObjectCreationExpr) fd.getVariables().get(0).getInit();
                if (oc.getAnonymousClassBody() != null) {
                    MethodDeclaration md = (MethodDeclaration) oc.getAnonymousClassBody().get(0);
                    Map<String, String> methodFieldMap = new HashMap<>(fieldMap);
                    MethodDeclarationVertex threadMethod = (MethodDeclarationVertex) createMethod(md, methodFieldMap);
                    service.saveEdge(threadClass.getId(), threadMethod.getId(), Relation.PARNET);
                    createMethodBody(md, threadMethod, methodFieldMap);
                    mdNIdPair.put(type, threadMethod);
                } else if (oc.getArgs() != null && oc.getArgs().get(0) instanceof ObjectCreationExpr) { //foo(new Interface(){})
                    MethodDeclaration md = (MethodDeclaration) ((ObjectCreationExpr) (oc.getArgs().get(0)))
                            .getAnonymousClassBody().get(0);
                    Map<String, String> methodFieldMap = new HashMap<>(fieldMap);
                    MethodDeclarationVertex threadMethod = (MethodDeclarationVertex) createMethod(md, methodFieldMap);
                    service.saveEdge(threadClass.getId(), threadMethod.getId(), Relation.PARNET);
                    createMethodBody(md, threadMethod, methodFieldMap);
                    mdNIdPair.put(type, threadMethod);
                }
            }

            //处理内部类
            List<ClassOrInterfaceDeclaration> innerClass = getInnerClass(body);// innerclass
            if (innerClass != null) {
                for (ClassOrInterfaceDeclaration inner : innerClass) {
                    Vertex innerClassVertex = createClass(inner);
                    service.saveEdge(clazzVertex.getId(), innerClassVertex.getId(), Relation.PARNET);
                    List<MethodDeclaration> methodsOfInnerClass = getMethodList(inner.getMembers());
                    if (inner.getExtends() != null && inner.getExtends().get(0).getName().startsWith("AsyncTask")) { // 处理AsyncTask的情况
                        Long pre = null, in = null, post = null;
                        for (MethodDeclaration method : methodsOfInnerClass) {
                            Map<String, String> methodFieldMap = new HashMap<>(fieldMap);
                            MethodDeclarationVertex innerMethod = (MethodDeclarationVertex) createMethod(method, methodFieldMap);
                            service.saveEdge(innerClassVertex.getId(), innerMethod.getId(), Relation.PARNET);
                            createMethodBody(method, innerMethod, methodFieldMap);
                            if (method.getName().startsWith("onPreExecute"))
                                pre = innerMethod.getId();
                            else if (method.getName().startsWith("doInBackground"))
                                in = innerMethod.getId();
                            else if (method.getName().startsWith("onPostExecute"))
                                post = innerMethod.getId();
                        }
                        service.saveEdge(pre, in, Relation.ORDER);
                        service.saveEdge(in, post, Relation.ORDER);
                        mdNIdPair.put(clazz.getName(), innerClassVertex);
                    } else if (inner.getExtends() != null
                            && inner.getExtends().get(0).getName().startsWith("Handler") || isThread(inner)) {
                        for (MethodDeclaration method : methodsOfInnerClass) {
                            Map<String, String> methodFieldMap = new HashMap<>(fieldMap);
                            MethodDeclarationVertex innerMethod = (MethodDeclarationVertex) createMethod(method, methodFieldMap);
                            service.saveEdge(innerClassVertex.getId(), innerMethod.getId(), Relation.PARNET);
                            createMethodBody(method, innerMethod, methodFieldMap);
                            mdNIdPair.put(inner.getName(), innerMethod);
                        }
                    } else {
                        for (MethodDeclaration method : methodsOfInnerClass) {
                            Map<String, String> methodFieldMap = new HashMap<>(fieldMap);
                            MethodDeclarationVertex innerMethod = (MethodDeclarationVertex) createMethod(method, methodFieldMap);
                            service.saveEdge(innerClassVertex.getId(), innerMethod.getId(), Relation.PARNET);
                            createMethodBody(method, innerMethod, methodFieldMap);
                            listenerMethodVertexPair.put(inner.getName() + "." + method.getName(), innerMethod);

                        }
                    }
                }
            }

            List<MethodDeclaration> methods = getMethodList(body);// method
            for (MethodDeclaration method : methods) {
                Map<String, String> methodFieldMap = new HashMap<>(fieldMap);
                MethodDeclarationVertex methodVertex = (MethodDeclarationVertex) createMethod(method, methodFieldMap);
                service.saveEdge(clazzVertex.getId(), methodVertex.getId(), Relation.PARNET);
                String methodName = packageClass + "." + method.getName();
                createMethodBody(method, methodVertex, methodFieldMap);
                mdNIdPair.put(methodName, methodVertex);
            }

            if (clazz.getExtends() != null && clazz.getExtends().get(0).getName().startsWith("AsyncTask")) {
                String methodName = packageClass + ".execute";
                mdNIdPair.put(methodName, clazzVertex);
            }
            System.out.println(javaFile.getAbsolutePath() + " completed!");
        }
    }

    private Vertex createPackage(Vertex pjVertex, CompilationUnit cu) {
        try {
            String pgName = cu.getPackage().getName().toString();
            if (!pgIdPair.containsKey(pgName)) {
                Vertex pg = new Vertex();
                pg.addLabel(projectName);
                pg.addLabel(EnumVertexLabelType.PACKAGE.getValue());
                pg.setCorrespondingPackage(pgName);
                pg.setName(pgName);
                service.saveVertex(pg);
                service.saveEdge(pjVertex.getId(), pg.getId(), Relation.PARNET);
                pgIdPair.put(pgName, pg);
                return pgIdPair.get(pgName);
            } else
                return pgIdPair.get(pgName);
        } catch (NullPointerException e) {
            return pjVertex;
        }
    }

    /**
     * @param clazz class结构
     * @return class的Id
     */
    private Vertex createClass(ClassOrInterfaceDeclaration clazz) {
        if (clazz == null)
            return null;
        String className = clazz.getName();

        if (clazz.getExtends() != null) {
            String extend = clazz.getExtends().stream().map(Node::toString).collect(Collectors.joining(","));
            className = className + " extends " + extend;
            extendsClass.addAll(clazz.getExtends().stream().map(Node::toString).collect(Collectors.toList()));
        }
        if (clazz.getImplements() != null) {
            String impl = clazz.getImplements().stream().map(Node::toString).collect(Collectors.joining(","));
            className = className + " implements " + impl;
        }
        Vertex classNode = new Vertex(EnumVertexLabelType.CLASSORINTERFACE.getValue(), className);
        classNode.addLabel(EnumVertexLabelType.CLASSORINTERFACE.getValue());
        classNode.addLabel(projectName);
        classNode.setName(className);
        classNode.setCorrespondingPackage(pgName);
        classNode.setCorrespondingClass(this.className);
        service.saveVertex(classNode);
        return classNode;
    }

    /**
     * 创建method节点
     *
     * @param methodDeclaration
     * @return
     */
    private Vertex createMethod(MethodDeclaration methodDeclaration, Map<String, String> map) {
        MethodDeclarationVertex methodNode = new MethodDeclarationVertex();
        methodNode.addLabel(EnumVertexLabelType.METHODDECLARATION.getValue());
        methodNode.addLabel(projectName);
        String methodName = methodDeclaration.getName();
        methodNode.setName(methodName);
        methodNode.setCorrespondingPackage(pgName);
        methodNode.setCorrespondingClass(className);
        if (methodDeclaration.getParameters() != null) {
            List<String> params = new ArrayList<>();
            for (Parameter parameter : methodDeclaration.getParameters()) {
                map.put(parameter.getId().getName(), parameter.getType().toString());
                map.put("this." + parameter.getId().getName(), parameter.getType().toString());
                params.add(parameter.getType().toString());
            }
            methodNode.setParameters(params.stream().collect(Collectors.joining(",")));
        }
        if (methodName.startsWith("on")) {
            methodNode.setCallBackFlag(1);
            methodNode.setRelevantView(className);
            if (!extendsClass.isEmpty() || !implInterface.isEmpty()) {
                methodNode.setInheritFlag(1);
                methodNode.setRelevantBaseView(FileUtil.base(extendsClass, implInterface));
            }
            try {
                if (methodDeclaration.getParentNode() instanceof ObjectCreationExpr) {
                    ObjectCreationExpr obce = (ObjectCreationExpr) methodDeclaration.getParentNode();
                    if (obce.getParentNode() instanceof MethodCallExpr) {
                        String scope = ((MethodCallExpr) obce.getParentNode()).getScope().toString();
                        if (map.containsKey(scope)) {
                            String type = map.get(scope);
                            methodNode.setRelevantView(map.get(scope));
                            if (inherit.containsKey(type)) {
                                methodNode.setInheritFlag(1);
                                methodNode.setRelevantBaseView(inherit.get(map.get(scope)));
                            } else {
                                methodNode.setInheritFlag(null);
                                methodNode.setRelevantBaseView(null);
                            }
                        }
                    }
                }
            } catch (Exception e) {

            }
        } else {
            methodNode.setCallBackFlag(0);
        }
        service.saveVertex(methodNode);
        return methodNode;
    }

    private void createMethodBody(MethodDeclaration method, Vertex methodVertex, Map<String, String> map) {
        Long methodId = methodVertex.getId();
        Vertex vertex = new Vertex();
        vertex.addLabel(EnumVertexLabelType.BLOCK.getValue());
        vertex.addLabel(projectName);
        vertex.setCorrespondingClass(className);
        vertex.setCorrespondingPackage(pgName);
        vertex.setName(EnumVertexLabelType.BLOCK.getValue());
        service.saveVertex(vertex);
        service.saveEdge(methodId, vertex.getId(), Relation.PARNET);
        BlockStmt blockStmt = method.getBody();// 有可能是抽象类或接口声明的方法，此时body为空
        if (blockStmt == null)
            return;
        List<Statement> stats = blockStmt.getStmts();
        if (stats == null)
            stats = new ArrayList<>();
        Vertex pre = null;
        for (Statement stmt : stats) {
            Vertex v = create(stmt, map, new HashMap<>());
            service.saveEdge(vertex.getId(), v.getId(), Relation.PARNET);
            if (pre != null) {
                service.saveEdge(pre.getEndId() != null ? pre.getEndId() : pre.getId(), v.getId(), Relation.ORDER);
                if(pre instanceof MethodCallExprVertex)
                    ((MethodCallExprVertex) pre).setNextVertexId(v.getId());
            }
            if(stmt == stats.get(stats.size() - 1))
                ((MethodDeclarationVertex) methodVertex).setFinalVertex(v);
            pre = v;
        }
        ((MethodDeclarationVertex) methodVertex).setBlockId(vertex.getId());

    }

    /**
     * @param node
     * @param fieldMap<变量名：类型名>
     * @param varMap<变量名：变量赋值位置>
     * @return
     */
    private Vertex create(Node node, Map<String, String> fieldMap, Map<String, VarNode> varMap) {
        Vertex v = null;
        List<String> sortList = null; // 每个语句中的变量名列表
        if (node == null)
            return new Vertex();
        if (node instanceof EnclosedExpr) {
            EnclosedExpr enclosedExpr = (EnclosedExpr) node;
            v = create(enclosedExpr.getInner(), fieldMap, varMap);
        } else if (node instanceof ExpressionStmt) {
            ExpressionStmt expressionStmt = (ExpressionStmt) node;
            v = create(expressionStmt.getExpression(), fieldMap, varMap);
        } else if (node instanceof ReturnStmt) {
            String noString = node.toString().substring(0, node.toString().length() - 1);
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.RETURNSTMT.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingPackage(className);
            v.setName(noString);
            service.saveVertex(v);
        } else if (node instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.BINARYEXPR.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(convertType(binaryExpr, fieldMap));
            service.saveVertex(v);
            sortList = new ArrayList<>();
            dataDependency(fieldMap, varMap, v, sortList, binaryExpr);
        } else if (node instanceof StringLiteralExpr) {
            StringLiteralExpr sle = (StringLiteralExpr) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.ATOM.getValue());
            v.setName(sle.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            service.saveVertex(v);
        } else if (node instanceof AssignExpr) {
            AssignExpr assignExpr = (AssignExpr) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.ASSIGNEXPR.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(convertType(assignExpr, fieldMap));
            service.saveVertex(v);
            sortList = new ArrayList<>();
            dataDependency(fieldMap, varMap, v, sortList, assignExpr);
        } else if (node instanceof VariableDeclarationExpr) {
            VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.VARIBLEDECLARATIONEXPR.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(convertType(vde, fieldMap));
            service.saveVertex(v);
            String type = vde.getType().toString();
            if (type.equals("Runnable") || type.equals("Thread")) {
                if (vde.getVars() != null && vde.getVars().get(0) != null) {
                    ObjectCreationExpr oce = (ObjectCreationExpr) vde.getVars().get(0).getInit();
                    if (oce.getAnonymousClassBody() != null) {
                        MethodDeclaration md = (MethodDeclaration) oce.getAnonymousClassBody().get(0);
                        MethodDeclarationVertex anonMethod = (MethodDeclarationVertex) createMethod(md, fieldMap);
                        createMethodBody(md, anonMethod, fieldMap);
                        service.saveEdge(v.getId(), anonMethod.getId(), Relation.PARNET);
                        mdNIdPair.put(type, anonMethod);
                    }
                }
            }
            sortList = new ArrayList<>();
            dataDependency(fieldMap, varMap, v, sortList, vde);
        } else if (node instanceof BlockStmt) { // BlockStmt {}
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.BLOCKSTMT.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(EnumVertexLabelType.BLOCKSTMT.getValue());
            service.saveVertex(v);
            pcBlockStmt(v, node, fieldMap, varMap);
        } else if (node instanceof IfStmt) {
            v = new Vertex();
            IfStmt ifStmt = (IfStmt) node;
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.BRANCH.getValue());
            v.setCorrespondingClass(className);
            v.setCorrespondingPackage(pgName);
            v.setName(EnumVertexLabelType.IFSTMT.getValue());
            service.saveVertex(v);

            Vertex end = new Vertex();
            end.addLabel(projectName);
            end.addLabel(EnumVertexLabelType.END.getValue());
            end.setCorrespondingClass(className);
            end.setCorrespondingPackage(pgName);
            end.setName("end");
            end.setStartId(v.getId());
            service.saveVertex(end);
            v.setEndId(end.getId());

            Expression condition = ifStmt.getCondition();
            Vertex conditionNN = create(condition, fieldMap, varMap);
            service.saveEdge(v.getId(), conditionNN.getId(), Relation.PARNET);
            Statement thenStmt = ifStmt.getThenStmt();
            Vertex thenNN = create(thenStmt, fieldMap, varMap);
            service.saveEdge(v.getId(), thenNN.getId(), Relation.PARNET);
            service.saveEdge(conditionNN.getId(), thenNN.getId(), Relation.TRUE); // 添加控制依赖
            Statement elseStmt = ifStmt.getElseStmt();
            if (elseStmt != null) {
                Vertex elseNN = null;
                if (elseStmt instanceof IfStmt) { // 处理else if语句
                    elseNN = create(elseStmt, fieldMap, varMap);
                    service.saveEdge(v.getId(), elseNN.getId(), Relation.PARNET);
                    service.saveEdge(conditionNN.getId(), elseNN.getId(), Relation.FALSE);
                    service.saveEdge(elseNN.getEndId(), end.getId(), Relation.PARNET);
                } else {
                    elseNN = new Vertex();
                    elseNN.addLabel(projectName);
                    elseNN.addLabel(EnumVertexLabelType.ELSE.getValue());
                    elseNN.setName(EnumVertexLabelType.ELSE.getValue());
                    elseNN.setCorrespondingPackage(pgName);
                    elseNN.setCorrespondingClass(className);
                    service.saveVertex(elseNN);
                    service.saveEdge(v.getId(), elseNN.getId(), Relation.PARNET);
                    service.saveEdge(conditionNN.getId(), elseNN.getId(), Relation.FALSE);
                    pcBlockStmt(elseNN, elseStmt, fieldMap, varMap);
                    service.saveEdge(elseNN.getEndId(), end.getId(), Relation.PARNET);
                }
            }

            service.saveEdge(conditionNN.getEndId(), end.getId(), Relation.PARNET);
            service.saveEdge(thenNN.getEndId(), end.getId(), Relation.PARNET);
        } else if (node instanceof SwitchStmt) {
            SwitchStmt switchStmt = (SwitchStmt) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.BRANCH.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(EnumVertexLabelType.SWITCHSTMT.getValue());
            service.saveVertex(v);

            Vertex end = new Vertex();
            end.addLabel(projectName);
            end.addLabel(EnumVertexLabelType.END.getValue());
            end.setCorrespondingClass(className);
            end.setCorrespondingPackage(pgName);
            end.setName("end");
            end.setStartId(v.getId());
            service.saveVertex(end);
            v.setEndId(end.getId());

            Vertex condition = create(switchStmt.getSelector(), fieldMap, varMap);
            service.saveEdge(v.getId(), condition.getId(), Relation.PARNET);
            service.saveEdge(condition.getEndId(), end.getId(), Relation.PARNET);
            List<SwitchEntryStmt> seStmts = switchStmt.getEntries();
            if (seStmts != null) {
                for (SwitchEntryStmt seStmt : seStmts) {
                    Vertex entry = create(seStmt, fieldMap, varMap);
                    service.saveEdge(v.getId(), entry.getId(), Relation.PARNET);
                    service.saveEdge(entry.getEndId(), end.getId(), Relation.PARNET);
                    if (!entry.getName().equals("default"))
                        service.saveEdge(condition.getId(), entry.getId(), Relation.EQUALS);
                    else
                        service.saveEdge(condition.getId(), entry.getId(), Relation.DEFAULT);
                    service.saveEdge(entry.getEndId(), end.getId(), Relation.PARNET);
                }
            }
        } else if (node instanceof SwitchEntryStmt) {
            SwitchEntryStmt seStmt = (SwitchEntryStmt) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.SWITCHENTRY.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(seStmt.getLabel() == null ? "default" : convertType(seStmt.getLabel(), fieldMap));
            service.saveVertex(v);

            List<Statement> stmts = seStmt.getStmts();
            if (stmts == null)
                stmts = new ArrayList<>();
            blockProcess(v, fieldMap, varMap, stmts);
        } else if (node instanceof DoStmt) {
            DoStmt doStmt = (DoStmt) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.LOOP.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(EnumVertexLabelType.DOWHILESTMT.getValue());
            service.saveVertex(v);

            Vertex end = new Vertex();
            end.addLabel(projectName);
            end.addLabel(EnumVertexLabelType.END.getValue());
            end.setCorrespondingClass(className);
            end.setCorrespondingPackage(pgName);
            end.setName("end");
            end.setStartId(v.getId());
            service.saveVertex(end);
            v.setEndId(end.getId());

            Vertex condition = create(doStmt.getCondition(), fieldMap, varMap);
            service.saveEdge(v.getId(), condition.getId(), Relation.PARNET);
            Vertex body = create(doStmt.getBody(), fieldMap, varMap);
            service.saveEdge(v.getId(), body.getId(), Relation.PARNET);
            service.saveEdge(condition.getId(), body.getId(), Relation.TRUE);
            service.saveEdge(body.getId(), condition.getId(), Relation.CDEPENDENCY);

            service.saveEdge(condition.getEndId(), end.getId(), Relation.PARNET);
            service.saveEdge(body.getEndId(), end.getId(), Relation.PARNET);

        } else if (node instanceof WhileStmt) { // while语句
            WhileStmt whileStmt = (WhileStmt) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.LOOP.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(EnumVertexLabelType.DOWHILESTMT.getValue());
            service.saveVertex(v);

            Vertex end = new Vertex();
            end.addLabel(projectName);
            end.addLabel(EnumVertexLabelType.END.getValue());
            end.setCorrespondingClass(className);
            end.setCorrespondingPackage(pgName);
            end.setName("end");
            end.setStartId(v.getId());
            service.saveVertex(end);
            v.setEndId(end.getId());

            Vertex condition = create(whileStmt.getCondition(), fieldMap, varMap);
            service.saveEdge(v.getId(), condition.getId(), Relation.PARNET);
            Vertex body = create(whileStmt.getBody(), fieldMap, varMap);
            service.saveEdge(v.getId(), body.getId(), Relation.PARNET);
            service.saveEdge(condition.getId(), body.getId(), Relation.TRUE);
            service.saveEdge(body.getId(), condition.getId(), Relation.CDEPENDENCY);

            service.saveEdge(condition.getEndId(), end.getId(), Relation.PARNET);
            service.saveEdge(body.getEndId(), end.getId(), Relation.PARNET);

        } else if (node instanceof ForStmt) { // for语句
            ForStmt forStmt = (ForStmt) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.LOOP.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(EnumVertexLabelType.FORSTMT.getValue());
            service.saveVertex(v);

            Vertex end = new Vertex();
            end.addLabel(projectName);
            end.addLabel(EnumVertexLabelType.END.getValue());
            end.setCorrespondingClass(className);
            end.setCorrespondingPackage(pgName);
            end.setName("end");
            end.setStartId(v.getId());
            service.saveVertex(end);
            v.setEndId(end.getId());

            String initstr = "";
            if (forStmt.getInit() != null) {
                initstr = forStmt.getInit().stream().map((n) -> convertType(n, fieldMap))
                        .collect(Collectors.joining(","));
            }
            Vertex init = new Vertex(); //init
            init.addLabel(projectName);
            init.addLabel(EnumVertexLabelType.FORINIT.getValue());
            init.setCorrespondingPackage(pgName);
            init.setCorrespondingClass(className);
            init.setName(initstr);
            service.saveVertex(init);
            service.saveEdge(v.getId(), init.getId(), Relation.PARNET);

            Vertex cmp = create(forStmt.getCompare(), fieldMap, varMap); // compare
            if (cmp != null) {
                service.saveEdge(v.getId(), cmp.getId(), Relation.PARNET);
                service.saveEdge(init.getId(), cmp.getId(), Relation.CDEPENDENCY);
            }

            Vertex body = create(forStmt.getBody(), fieldMap, varMap); // body
            if (body != null) {
                service.saveEdge(v.getId(), body.getId(), Relation.PARNET);
                if (cmp != null)
                    service.saveEdge(cmp.getId(), body.getId(), Relation.TRUE);
            }

            String updateStr = "";
            Vertex update = null;
            if (forStmt.getUpdate() != null) {
                updateStr = forStmt.getUpdate().stream().map((n) -> convertType(n, fieldMap))
                        .collect(Collectors.joining(","));
                update = new Vertex();
                update.addLabel(projectName);
                update.addLabel(EnumVertexLabelType.FORUPDATE.getValue());
                update.setCorrespondingPackage(pgName);
                update.setCorrespondingClass(className);
                update.setName(updateStr);
                service.saveVertex(update);
            }
            if (update != null) {
                service.saveEdge(v.getId(), update.getId(), Relation.PARNET);
                service.saveEdge(update.getEndId(), end.getId(), Relation.PARNET);
            }
            if (body != null && update != null) {
                service.saveEdge(body.getId(), update.getId(), Relation.CDEPENDENCY);
                service.saveEdge(body.getEndId(), end.getId(), Relation.PARNET);
            }
            if (update != null && cmp != null) {
                service.saveEdge(update.getId(), cmp.getId(), Relation.CDEPENDENCY);
                service.saveEdge(cmp.getEndId(), end.getId(), Relation.PARNET);
            }
            service.saveEdge(init.getEndId(), end.getId(), Relation.PARNET);
        } else if (node instanceof ForeachStmt) { // foreach语句
            ForeachStmt foreachStmt = (ForeachStmt) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.LOOP.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(EnumVertexLabelType.FOREACHSTMT.getValue());
            service.saveVertex(v);

            Vertex end = new Vertex();
            end.addLabel(projectName);
            end.addLabel(EnumVertexLabelType.END.getValue());
            end.setCorrespondingClass(className);
            end.setCorrespondingPackage(pgName);
            end.setName("end");
            end.setStartId(v.getId());
            service.saveVertex(end);
            v.setEndId(end.getId());

            Vertex vdNN = create(foreachStmt.getVariable(), fieldMap, varMap);
            service.saveEdge(v.getId(), vdNN.getId(), Relation.PARNET);
            Vertex iterable = create(foreachStmt.getIterable(), fieldMap, varMap);
            service.saveEdge(v.getId(), iterable.getId(), Relation.PARNET);
            Vertex body = create(foreachStmt.getBody(), fieldMap, varMap);
            service.saveEdge(v.getId(), body.getId(), Relation.PARNET);
            service.saveEdge(vdNN.getId(), iterable.getId(), Relation.IN);
            service.saveEdge(vdNN.getId(), body.getId(), Relation.CDEPENDENCY);
            service.saveEdge(body.getId(), vdNN.getId(), Relation.CDEPENDENCY);

            service.saveEdge(vdNN.getEndId(), end.getId(), Relation.PARNET);
            service.saveEdge(iterable.getEndId(), end.getId(), Relation.PARNET);
            service.saveEdge(body.getEndId(), end.getId(), Relation.PARNET);

        } else if (node instanceof TryStmt) {
            TryStmt tryStmt = (TryStmt) node;
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.TRYSTMT.getValue());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            v.setName(EnumVertexLabelType.TRYSTMT.getValue());
            service.saveVertex(v);

            Vertex end = new Vertex();
            end.addLabel(projectName);
            end.addLabel(EnumVertexLabelType.END.getValue());
            end.setCorrespondingClass(className);
            end.setCorrespondingPackage(pgName);
            end.setName("end");
            end.setStartId(v.getId());
            service.saveVertex(end);
            v.setEndId(end.getId());

            Vertex block = create(tryStmt.getTryBlock(), fieldMap, varMap);
            service.saveEdge(v.getId(), block.getId(), Relation.PARNET);
            service.saveEdge(block.getEndId(), end.getId(), Relation.PARNET);
            List<CatchClause> catchs = tryStmt.getCatchs();
            if (catchs != null) {
                Vertex catchStmt = new Vertex();
                catchStmt.addLabel(projectName);
                catchStmt.addLabel(EnumVertexLabelType.CATCHSTMT.getValue());
                catchStmt.setCorrespondingPackage(pgName);
                catchStmt.setCorrespondingClass(className);
                catchStmt.setName(EnumVertexLabelType.CATCHSTMT.getValue());

                service.saveVertex(catchStmt);
                service.saveEdge(v.getId(), catchStmt.getId(), Relation.PARNET);

                Vertex pre = null;
                for (CatchClause cc : catchs) {
                    Vertex ccNode = new Vertex();
                    ccNode.addLabel(projectName);
                    ccNode.addLabel(EnumVertexLabelType.CATCHCLAUSE.getValue());
                    ccNode.setCorrespondingPackage(pgName);
                    ccNode.setCorrespondingClass(className);
                    ccNode.setName(cc.toString());
                    service.saveVertex(ccNode);
                    service.saveEdge(catchStmt.getId(), ccNode.getId(), Relation.PARNET);
                    Vertex catchBlock = create(cc.getCatchBlock(), fieldMap, varMap);
                    service.saveEdge(ccNode.getId(), catchBlock.getId(), Relation.PARNET);
                    if (pre != null)
                        service.saveEdge(pre.getId(), ccNode.getId(), Relation.ORDER);
                    pre = ccNode;
                }
            }
            if (tryStmt.getFinallyBlock() != null) {
                Vertex finallyNode = new Vertex();
                finallyNode.addLabel(projectName);
                finallyNode.addLabel(EnumVertexLabelType.FINALLY.getValue());
                finallyNode.setCorrespondingPackage(pgName);
                finallyNode.setCorrespondingClass(className);
                finallyNode.setName(EnumVertexLabelType.FINALLY.getValue());
                service.saveVertex(finallyNode);
                service.saveEdge(v.getId(), finallyNode.getId(), Relation.PARNET);
                Vertex finallyBlock = create(tryStmt.getFinallyBlock(), fieldMap, varMap);
                service.saveEdge(finallyNode.getId(), finallyBlock.getId(), Relation.PARNET);
            }
        } else if (node instanceof MethodCallExpr) {
            MethodCallExpr methodCallExpr = (MethodCallExpr) node;
            if (!(methodCallExpr.getScope() != null && methodCallExpr.getScope().toString().startsWith("Log"))) { // 不是log方法
                MethodCallExprVertex vertex = new MethodCallExprVertex();
                vertex.addLabel(projectName);
                vertex.addLabel(EnumVertexLabelType.METHODCALLEXPR.getValue());
                vertex.setCorrespondingPackage(pgName);
                vertex.setCorrespondingClass(className);
                vertex.setName(methodCallExpr.getName());

                if (methodCallExpr.getScope() == null) {    // 对于scope为null的方法调用，判断该方法调用是否在本类中或者是父类(这里的父类必须是用户自定义的)中
                    boolean api = false;
                    if (!classMethodPair.get(className).contains(methodCallExpr.getName())) {
                        for (String ext : extendsClass) {
                            if (classMethodPair.containsKey(ext)) {
                                api = classMethodPair.get(ext).contains(methodCallExpr.getName());
                                if (api)
                                    break;
                            }
                        }
                        if (!api) {
                            for (String impl : implInterface) {
                                if (classMethodPair.containsKey(impl)) {
                                    api = classMethodPair.get(impl).contains(methodCallExpr.getName());
                                }
                            }
                        }
                    }
                    if (api)
                        vertex.setApiFlag(0);
                    else
                        vertex.setApiFlag(1);

                    vertex.setObjectClass(className);
                    if (inherit.containsKey(className)) {
                        vertex.setInheritFlag(1);
                        vertex.setObjectBaseClass(inherit.get(className));
                    } else
                        vertex.setInheritFlag(0);
                } else if (methodCallExpr.getScope().toString().equals("this")) {
                    vertex.setApiFlag(0);
                    vertex.setObjectClass(className);
                } else if (methodCallExpr.getScope().toString().equals("super")) {
                    if (methodCallExpr.getName().startsWith("on")) {
                        vertex.setApiFlag(1);
                        vertex.setObjectClass(inherit.get(className));
                    } else {
                        if (inherit.containsKey(className)) {
                            Set<String> methods = classMethodPair.get(inherit.get(className));
                            if (methods.contains(methodCallExpr.getName())) {
                                vertex.setApiFlag(0);
                            } else {
                                vertex.setApiFlag(1);
                            }
                            vertex.setObjectClass(inherit.get(className));
                        } else {
                            vertex.setApiFlag(1);
                            vertex.setObjectClass(extendsClass.get(0));
                        }
                    }
                } else {
                    String name = null;
                    if (methodCallExpr.getScope() instanceof NameExpr) {
                        NameExpr nameExpr = (NameExpr) methodCallExpr.getScope();
                        name = nameExpr.getName();
                    } else if (methodCallExpr.getScope() instanceof FieldAccessExpr) {
                        FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) methodCallExpr.getScope();
                        name = fieldAccessExpr.getField();
                    }
                    if (name != null) {
                        if (!fieldMap.containsKey(name)) {
                            if (!classMethodPair.containsKey(name))
                                vertex.setApiFlag(1);
                            else
                                vertex.setApiFlag(0);
                            vertex.setObjectClass(name);
                        } else {
                            String type = fieldMap.get(name); //type就是a.b()中a的类型
                            if (classMethodPair.containsKey(type)) {//如果能找到它的类型
                                Set<String> methods = classMethodPair.get(type);
                                if (methods.contains(methodCallExpr.getName())) {//如果发现这个方法在别的类中被声明过
                                    vertex.setApiFlag(0);
                                    vertex.setObjectClass(type);
                                } else {//没有在别的类中声明过就认为是api
                                    vertex.setApiFlag(1);
                                    vertex.setObjectClass(type);
                                    if (inherit.containsKey(type)) {
                                        vertex.setInheritFlag(1);
                                        vertex.setObjectBaseClass(inherit.get(type));
                                    } else {
                                        vertex.setInheritFlag(0);
                                    }
                                }
                            } else {//这个类型对应的类不是用户自定义的，所以认为是api
                                vertex.setApiFlag(1);
                                vertex.setObjectClass(type);
                                vertex.setInheritFlag(0);
                            }
                        }
                    }
                }

                try {
                    vertex.setParameters(methodCallExpr.getArgs().stream().map((n) -> convertType(n, fieldMap, new HashMap<>())).collect(Collectors.joining(",")));
                } catch (NullPointerException ignored) {

                }

                //处理类似于a.setOnClick(new ClickListener(){})
                if (methodCallExpr.getArgs() != null && methodCallExpr.getArgs().get(0) instanceof ObjectCreationExpr) {
                    service.saveVertex(vertex);
                    ObjectCreationExpr oce = (ObjectCreationExpr) methodCallExpr.getArgs().get(0);
                    Vertex classOrInterface = new Vertex();
                    classOrInterface.addLabel(projectName);
                    classOrInterface.addLabel(EnumVertexLabelType.CLASSORINTERFACE.getValue());
                    classOrInterface.setCorrespondingPackage(pgName);
                    classOrInterface.setCorrespondingClass(className);
                    classOrInterface.setName(oce.getType().getName());
                    service.saveVertex(classOrInterface);
                    service.saveEdge(vertex.getId(), classOrInterface.getId(), Relation.PARNET);
                    if (oce.getAnonymousClassBody() != null) {
                        List<MethodDeclaration> mds = getMethodList(oce.getAnonymousClassBody());
                        for (MethodDeclaration md : mds) {
                            Map<String, String> tempMap = new HashMap<>(fieldMap);
                            MethodDeclarationVertex method = (MethodDeclarationVertex) createMethod(md, tempMap);
                            if (oce.getType().getName().equals("Runnable")) {
                                if (md.getName().equals("run")) {
                                    service.saveEdge(vertex.getId(), method.getId(), Relation.THREAD);
                                }
                            }
                            service.saveEdge(classOrInterface.getId(), method.getId(), Relation.PARNET);
                            createMethodBody(md, method, tempMap);
                        }

                    }
                }

                MethodCall mc = new MethodCall();

                if (methodCallExpr.getScope() == null) {
                    mc.setName(packageClass + "." + methodCallExpr.getName());
                } else {
                    if (methodCallExpr.getName().equals("execute")) { //Asyntask
                        if (methodCallExpr.getScope() instanceof ObjectCreationExpr) {
                            mc.setName(((ObjectCreationExpr) methodCallExpr.getScope()).getType().getName());
                        } else {
                            String type = convertType(node, fieldMap).split("[.]")[0];
                            mc.setName(type);
                        }
                        mc.setEdge(Relation.ASYNCTASK);
                    } else if (methodCallExpr.getName().equals("start")) {
                        if (methodCallExpr.getScope() instanceof ObjectCreationExpr) {
                            ObjectCreationExpr oc = (ObjectCreationExpr) methodCallExpr.getScope();
                            if (oc.getArgs() != null && oc.getArgs().get(0) instanceof ObjectCreationExpr) {
                                ObjectCreationExpr ocexpr = (ObjectCreationExpr) oc.getArgs().get(0);
                                if (ocexpr.getAnonymousClassBody() != null) {
                                    vertex.setName("Thread.start()");
                                    service.saveVertex(vertex);
                                    MethodDeclaration md = (MethodDeclaration) ((ObjectCreationExpr) (oc.getArgs()
                                            .get(0))).getAnonymousClassBody().get(0);
                                    MethodDeclarationVertex anonMethod = (MethodDeclarationVertex) createMethod(md, fieldMap);
                                    createMethodBody(md, anonMethod, fieldMap);
                                    service.saveEdge(vertex.getId(), anonMethod.getId(), Relation.PARNET);
                                    service.saveEdge(vertex.getId(), anonMethod.getId(), Relation.THREAD);
                                }
                            } else
                                mc.setName(((ObjectCreationExpr) methodCallExpr.getScope()).getType().getName());
                        } else {
                            String type = convertType(node, fieldMap).split("[.]")[0];
                            mc.setName(type);
                        }
                        mc.setEdge(Relation.THREAD);
                    } else if (methodCallExpr.getName().equals("sendMessage")) {
                        if (methodCallExpr.getScope() instanceof ObjectCreationExpr) {
                            mc.setName(((ObjectCreationExpr) methodCallExpr.getScope()).getType().getName());
                        } else {
                            String type = convertType(node, fieldMap).split("[.]")[0];
                            mc.setName(type);
                        }
                        mc.setEdge(Relation.MESSAGE);

                    } else if ((methodCallExpr.getName().equals("post") || methodCallExpr.getName().equals("postDelay"))
                            && methodCallExpr.getArgs() != null && methodCallExpr.getArgs().get(0) != null) {
                        String type = fieldMap.get(methodCallExpr.getArgs().get(0).toString());
                        if (type != null && type.equals("Runnable")) {
                            mc.setName("Runnable");
                            mc.setEdge(Relation.POST);
                        }
                    } else if (Pattern.matches("setOn.*Listener", methodCallExpr.getName())) {
                        try {
                            String param = methodCallExpr.getArgs().get(0).toString();
                            if (!param.equals("this")) {
                                String type = fieldMap.get(param);
                                Set<String> listenerMethod = listenerClassMethodPair.get(type);
                                mc.setName(listenerMethod.stream().collect(Collectors.joining(",")));
                            }
                        } catch (NullPointerException e) {

                        }
                    } else {
                        String str = convertType(node, fieldMap).split("[.]")[0];
                        for (String ipt : imports) {
                            String[] strs = ipt.split("[.]");
                            if (str.equals(strs[strs.length - 1])) {
                                mc.setName(ipt + "." + methodCallExpr.getName());
                            }
                        }
                    }
                }

                sortList = new ArrayList<>();
                if (vertex.getId() == null)
                    service.saveVertex(vertex);
                mc.setMethodCallVertex(vertex);
                dataDependency(fieldMap, varMap, vertex, sortList, methodCallExpr);

                methodCall.add(mc);

                return vertex;
            } else {
                v = new Vertex();
            }
        } else {
            v = new Vertex();
            v.addLabel(projectName);
            v.addLabel(EnumVertexLabelType.ATOM.getValue());
            v.setName(node.toString());
            v.setCorrespondingPackage(pgName);
            v.setCorrespondingClass(className);
            service.saveVertex(v);
        }

        return v;
    }

    /**
     * 处理if/for等语句的语句块，blockstmt或者expression
     *
     * @param
     * @param
     */
    private void pcBlockStmt(Vertex v, Node node, Map<String, String> map, Map<String, VarNode> varMap) {
        if (node instanceof BlockStmt) { // 如果是带括号的，则把这些语句并列起来，父节点就是if
            List<Statement> stats = ((BlockStmt) node).getStmts();
            if (stats != null) {
                blockProcess(v, map, varMap, stats);
            }
        } else {
            Vertex stmtNN = create(node, map, varMap);
            service.saveEdge(v.getId(), stmtNN.getId(), Relation.PARNET);
            v.setEndId(stmtNN.getId());
        }
    }

    private void blockProcess(Vertex v, Map<String, String> map, Map<String, VarNode> varMap, List<Statement> stats) {
        Vertex preNode = null;
        Vertex end = new Vertex();
        end.addLabel(projectName);
        end.addLabel(EnumVertexLabelType.END.getValue());
        end.setCorrespondingClass(className);
        end.setCorrespondingPackage(pgName);
        end.setName("end");
        end.setStartId(v.getId());
        if (!stats.isEmpty()) {
            service.saveVertex(end);
            v.setEndId(end.getId());
        }
        for (Statement stmt : stats) {
            Vertex stmtNN = create(stmt, map, varMap);
            if (v != null && stmtNN != null) {
                service.saveEdge(v.getId(), stmtNN.getId(), Relation.PARNET);
                service.saveEdge(stmtNN.getEndId(), end.getId(), Relation.PARNET);
                if (preNode != null) {
                    service.saveEdge(preNode.getEndId() != null ? preNode.getEndId() : preNode.getId(), stmtNN.getId(), Relation.ORDER);
                    if(preNode instanceof MethodCallExprVertex)
                        ((MethodCallExprVertex) preNode).setNextVertexId(stmtNN.getId());
                }
                preNode = stmtNN;
            }
        }
    }

    /**
     * @param fieldMap<变量名:变量类型>
     * @param varMap<变量名:变量声明赋值位置>
     * @param v<变量所在节点>
     * @param sortList<语句中变量列表>
     * @param node<语句node>
     */
    private void dataDependency(Map<String, String> fieldMap, Map<String, VarNode> varMap, Vertex v,
                                List<String> sortList, Node node) {
        if (node instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) node;
            sortNameExpr(binaryExpr, sortList);
            buildDependency(varMap, v, sortList);
        } else if (node instanceof AssignExpr) {
            AssignExpr assignExpr = (AssignExpr) node;
            dataDependency(fieldMap, varMap, v, sortList, assignExpr.getValue());
            VarNode varNode = new VarNode(v.getId(), fieldMap.get(assignExpr.getTarget().toString()));
            varMap.put(assignExpr.getTarget().toString(), varNode);
        } else if (node instanceof MethodCallExpr) {
            MethodCallExpr methodCallExpr = (MethodCallExpr) node;
            if (methodCallExpr.getArgs() == null)
                return;
            sortNameExpr(methodCallExpr, sortList);
            buildDependency(varMap, v, sortList);
        } else if (node instanceof NameExpr) {
            String name = ((NameExpr) node).getName();
            if (!varMap.containsKey(name))
                return;
            VarNode varNode = varMap.get(name);
            service.saveEdge(varNode.getId(), v.getId(),
                    new Edge(EnumRelationShipType.DDEPENDENCY.getValue(), varNode.getSignal(), "1"));
        } else if (node instanceof VariableDeclarationExpr) {
            VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
            sortNameExpr(vde, sortList);
            for (VariableDeclarator vd : vde.getVars()) {
                if (vd.getInit() == null)
                    continue;
                dataDependency(fieldMap, varMap, v, sortList, vd.getInit());
            }
            List<VariableDeclarator> vds = vde.getVars();
            for (int i = 0; i < vds.size(); i++) {
                VarNode varNode = new VarNode(v.getId(), fieldMap.get(vds.get(0).getId().toString()));
                varMap.put(vds.get(0).getId().toString(), varNode);
            }

        }
    }

    private void buildDependency(Map<String, VarNode> varMap, Vertex v, List<String> sortList) {
        for (int i = 0; i < sortList.size(); i++) {
            String var = sortList.get(i);
            if (!varMap.containsKey(var))
                continue;
            VarNode varNode = varMap.get(var);
            service.saveEdge(varNode.getId(), v.getId(),
                    new Edge(EnumRelationShipType.DDEPENDENCY.getValue(), varNode.getSignal(), i + ""));
        }
    }

    /**
     * @param node<语句node>
     * @param sortList<语句中变量列表>
     */
    private void sortNameExpr(Node node, List<String> sortList) {
        if (node == null)
            return;
        if (node instanceof AssignExpr) {
            AssignExpr assignExpr = (AssignExpr) node;
            sortNameExpr(assignExpr.getTarget(), sortList);
            sortNameExpr(assignExpr.getValue(), sortList);
        } else if (node instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) node;
            sortNameExpr(binaryExpr.getLeft(), sortList);
            sortNameExpr(binaryExpr.getRight(), sortList);
        } else if (node instanceof UnaryExpr) {
            UnaryExpr unaryExpr = (UnaryExpr) node;
            sortNameExpr(unaryExpr.getExpr(), sortList);
        } else if (node instanceof MethodCallExpr) {
            MethodCallExpr methodCallExpr = (MethodCallExpr) node;
            if (methodCallExpr.getArgs() != null)
                methodCallExpr.getArgs().forEach((n) -> sortNameExpr(n, sortList));
        } else if (node instanceof NameExpr) {
            NameExpr nameExpr = (NameExpr) node;
            sortList.add(nameExpr.getName());
        } else if (node instanceof VariableDeclarationExpr) {
            VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
            for (VariableDeclarator vd : vde.getVars())
                if (vd.getInit() != null)
                    sortNameExpr(vd.getInit(), sortList);
        }
    }

    private String convertType(Node node, Map<String, String> map) {
        return convertType(node, map, new HashMap<>());
    }

    private ClassOrInterfaceDeclaration getClass(CompilationUnit cu) {
        try {
            TypeDeclaration tp = cu.getTypes().stream().filter((n) -> (n instanceof ClassOrInterfaceDeclaration)).findFirst().get();
            return (ClassOrInterfaceDeclaration) tp;
        } catch (Exception e) {
            return null;
        }
    }

    private String convertType(Node node, Map<String, String> map, Map<String, Integer> indexMap) {
        String str = "";
        if (node == null)
            return "";
        if (node instanceof AssignExpr) {
            AssignExpr assignExpr = (AssignExpr) node;
            str = convertType(assignExpr.getTarget(), map, indexMap) + " "
                    + ConvertEnumUtil.getAssignOperator(assignExpr.getOperator()) + " "
                    + convertType(assignExpr.getValue(), map, indexMap);
        } else if (node instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) node;
            str = convertType(binaryExpr.getLeft(), map, indexMap) + " "
                    + ConvertEnumUtil.getBinaryOperator(binaryExpr.getOperator()) + " "
                    + convertType(binaryExpr.getRight(), map, indexMap);
        } else if (node instanceof UnaryExpr) {
            UnaryExpr unaryExpr = (UnaryExpr) node;
            str = ConvertEnumUtil.getUnaryOperator(unaryExpr.getOperator())
                    + convertType(unaryExpr.getExpr(), map, indexMap);
        } else if (node instanceof NameExpr) {
            NameExpr nameExpr = (NameExpr) node;
            if (map.containsKey(nameExpr.getName())) {
                String type = map.get(nameExpr.getName());
                if (indexMap.containsKey(type)) {
                    str = type + indexMap.get(type);
                    indexMap.put(type, indexMap.get(type) + 1);
                } else {
                    str = type;
                    indexMap.put(type, 1);
                }
            } else
                str = nameExpr.getName();
        } else if (node instanceof VariableDeclarationExpr) {
            VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
            String type = vde.getType().toString();
            vde.getVars().forEach((n) -> map.put(n.getId().getName(), type));
            str = vde.getVars().stream().map((n) -> (convertType(n, map, indexMap))).collect(Collectors.joining(","));

        } else if (node instanceof VariableDeclarator) {
            VariableDeclarator vd = (VariableDeclarator) node;
            String type = map.get(vd.getId().getName());
            if (indexMap.containsKey(type)) {
                if (vd.getInit() == null)
                    str = type + indexMap.get(type);
                else
                    str = type + indexMap.get(type) + " = " + convertType(vd.getInit(), map, indexMap);
                indexMap.put(type, indexMap.get(type) + 1);
            } else {
                if (vd.getInit() == null)
                    str = type;
                else
                    str = type + " = " + convertType(vd.getInit(), map, indexMap);
                indexMap.put(type, 1);
            }
        } else if (node instanceof MethodCallExpr) {
            MethodCallExpr methodCallExpr = (MethodCallExpr) node;
            if (methodCallExpr.getScope() != null)
                str = convertType(methodCallExpr.getScope(), map, indexMap) + ".";
            String args = "";
            if (methodCallExpr.getArgs() != null)
                if (methodCallExpr.getArgs().get(0) instanceof ObjectCreationExpr) {
                    args = "";
                } else {
                    args = methodCallExpr.getArgs().stream().map((n) -> convertType(n, map, indexMap))
                            .collect(Collectors.joining(","));
                }
            str = str + methodCallExpr.getName() + "(" + args + ")";
        } else {
            str = node.toString();
        }
        return str;
    }

    /**
     * 类中属性与类型的映射
     *
     * @param body
     * @return
     */
    private Map<String, String> getField(List<BodyDeclaration> body) {
        Map<String, String> map = new HashMap<>();
        List<FieldDeclaration> fds = body.stream().filter((n) -> (n instanceof FieldDeclaration))
                .map((n) -> (FieldDeclaration) n).collect(Collectors.toList());
        for (FieldDeclaration fd : fds) {
            String type = fd.getType().toString();
            for (VariableDeclarator vd : fd.getVariables()) {
                String var = vd.getId().getName();
                map.put(var, type);
                map.put("this." + var, type);
            }
        }
        return map;
    }

    /**
     * 从body中获取方法声明节点
     *
     * @param body
     * @return
     */
    private List<MethodDeclaration> getMethodList(List<BodyDeclaration> body) {
        List<MethodDeclaration> methods = null;
        methods = body.stream().filter((n) -> (n instanceof MethodDeclaration)).map((n) -> ((MethodDeclaration) n))
                .collect(Collectors.toList());
        return methods;
    }

    private List<FieldDeclaration> getThreadOrHandlerField(List<BodyDeclaration> body) {
        List<FieldDeclaration> list = new ArrayList<>();
        if (body == null)
            return list;
        List<FieldDeclaration> fds = body.stream().filter((n) -> (n instanceof FieldDeclaration))
                .map((n) -> (FieldDeclaration) n).collect(Collectors.toList());
        if (fds == null)
            return list;
        for (FieldDeclaration fd : fds) {
            if (!(fd.getType() instanceof ReferenceType))
                continue;
            ReferenceType rt = (ReferenceType) fd.getType();
            if (!(rt.getType() instanceof ClassOrInterfaceType))
                continue;
            String type = ((ClassOrInterfaceType) rt.getType()).getName();
            if (type.equals("Handler") || type.equals("Thread") || type.equals("Runnable")) {
                if (fd.getVariables() == null)
                    continue;
                if (fd.getVariables().get(0).getInit() == null
                        || !(fd.getVariables().get(0).getInit() instanceof ObjectCreationExpr))
                    continue;
                list.add(fd);
            }
        }
        return list;
    }

    /**
     * 添加函数调用链
     */
    private void callChain() {
        for (MethodCall mc : methodCall) {
            if (mc.getName() == null)
                continue;
            MethodCallExprVertex begin = (MethodCallExprVertex) mc.getMethodCallVertex();
            MethodDeclarationVertex endVertex = (MethodDeclarationVertex) mdNIdPair.get(mc.getName());
            if (begin != null && endVertex != null) {
                Long end = endVertex.getBlockId();
                service.saveEdge(begin.getId(), end, mc.getEdge());
                service.saveEdge(endVertex.getFinalVertex().getId(),begin.getNextVertexId(),Relation.RETURN);
            }
            String name = mc.getName();
            if(begin != null) {
                if (name.contains(",")) {
                    String[] listenerMethodNames = name.split(",");
                    for (String str : listenerMethodNames) {
                        if (listenerMethodVertexPair.containsKey(str)) {
                            Long end = listenerMethodVertexPair.get(str).getId();
                            service.saveEdge(begin.getId(), end, mc.getEdge());
                        }
                    }
                } else {
                    if (listenerMethodVertexPair.containsKey(name)) {
                        Long end = listenerMethodVertexPair.get(name).getId();
                        service.saveEdge(begin.getId(), end, mc.getEdge());
                    }
                }
            }
        }
    }

    private List<ClassOrInterfaceDeclaration> getInnerClass(List<BodyDeclaration> body) {
        List<ClassOrInterfaceDeclaration> clazz = null;
        clazz = body.stream().filter((n) -> (n instanceof ClassOrInterfaceDeclaration))
                .map((n) -> ((ClassOrInterfaceDeclaration) n)).collect(Collectors.toList());
        List<ClassOrInterfaceDeclaration> returnClazz = new ArrayList<>();
        if (clazz == null)
            return clazz;
        List<ClassOrInterfaceDeclaration> temp = new ArrayList<>(clazz);
        for (ClassOrInterfaceDeclaration ci : temp) {
            if (!isThread(ci)) {
                returnClazz.add(ci);
                clazz.remove(ci);
            }
        }
        returnClazz.addAll(clazz);
        return returnClazz;
    }

    private boolean isThread(ClassOrInterfaceDeclaration clazz) {
        return clazz.getExtends() != null && clazz.getExtends().get(0).getName().equals("Thread")
                || clazz.getImplements() != null && clazz.getImplements().stream().map(ClassOrInterfaceType::getName)
                .collect(Collectors.toList()).contains("Runnable");
    }


    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public static void main(String[] args) {
        String basePath = LoadProperties.get("BASEPATH") + args[0] + "/";
        String movePath = LoadProperties.get("BASEPATH") + "complete/";
        String badPath = LoadProperties.get("BASEPATH") + "bad_project/";
        File file = new File(basePath);
        Generate generate = null;
        if (file.isDirectory()) {
            String[] pj = file.list();
            for (String path : pj) {
                if (!path.startsWith(".")) {
                    File project = new File(basePath + path);
                    String projectName = path.replace("-", "_");
                    try {
                        generate = new Generate(projectName);
                        generate.analyzePJ(project);
                        project.renameTo(new File(movePath + path));
                        System.out.println(path + " has been moved to complete!---------------------------------");
                    } catch (Exception e) {
                        e.printStackTrace();
                        new VertexService().deleteProject(projectName);
                        project.renameTo(new File(badPath + path));
                        System.out.println(path + " has been moved to bad!---------------------------------");
                    }
                }

            }
        }
    }

    /*
    public static void main(String[] args) {
        String basePath = "/Users/xiyaoguo/Documents/work/ICSE/test/";
        File file = new File(basePath);
        Generate generate = null;
        if (file.isDirectory()) {
            String[] pj = file.list();
            for (String path : pj) {
                if (!path.startsWith(".")) {
                    File project = new File(basePath + path);
                    String projectName = path.replace("-", "_");
                    generate = new Generate(projectName);
                    generate.analyzePJ(project);
                }
            }
        }
    }*/


}
