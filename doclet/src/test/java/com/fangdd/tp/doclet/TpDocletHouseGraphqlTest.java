package com.fangdd.tp.doclet;

import com.sun.tools.javadoc.Main;

import java.io.IOException;

/**
 * @author ycoe
 * @date 18/1/5
 */
public class TpDocletHouseGraphqlTest {
    /**
     * maven仓库地址
     */
    private static final String mavenRepositoryPath = "/Users/xuwenzhen/Software/apache-maven-3.6.0/repository/";

    /**
     * 需要引入的包
     */
    private static final String[] jars = new String[]{
            "org/springframework/boot/spring-boot-autoconfigure/2.1.3.RELEASE/spring-boot-autoconfigure-2.1.3.RELEASE.jar",
            "org/springframework/boot/spring-boot/2.1.3.RELEASE/spring-boot-2.1.3.RELEASE.jar",
            "org/springframework/spring-web/5.1.5.RELEASE/spring-web-5.1.5.RELEASE.jar",
            "com/alibaba/fastjson/1.2.7/fastjson-1.2.7.jar",
            "org/springframework/spring-context/5.1.5.RELEASE/spring-context-5.1.5.RELEASE.jar",
            "org/springframework/spring-core/5.1.5.RELEASE/spring-core-5.1.5.RELEASE.jar",
            "org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar",
            "org/springframework/spring-beans/5.1.5.RELEASE/spring-beans-5.1.5.RELEASE.jar",
            "com/google/guava/guava/26.0-jre/guava-26.0-jre.jar",
            "com/fangdd/graphql/graphql-provider-remote/1.0.0-SNAPSHOT/graphql-provider-remote-1.0.0-SNAPSHOT.jar",
            "com/fangdd/graphql/spring-boot-starter-fdd-graphql-provider-tpdoc/1.0.0-SNAPSHOT/spring-boot-starter-fdd-graphql-provider-tpdoc-1.0.0-SNAPSHOT.jar",
            "com/fasterxml/jackson/core/jackson-annotations/2.9.0/jackson-annotations-2.9.0.jar",
            "org/hibernate/validator/hibernate-validator/6.0.14.Final/hibernate-validator-6.0.14.Final.jar"};

    private static String projectPath;

    public static void main(String[] args) throws IOException {
        System.setProperty("exporter", "console");
        projectPath = System.getProperty("user.dir");

        String projectSrcDirs = getProjectSrcDirs();
        System.out.println("扫描目录：" + projectSrcDirs);
        String[] docArgs = new String[]{
                "com.fangdd.graphql.house.controller",
                "-subpackages",
                projectPath + "/server/target/tp-server.jar",
                "-cp",
                getLibs(),
                "-sourcepath",
                projectSrcDirs
        };

        Main.execute("myJavadoc", TpDoclet.class.getName(), docArgs);
    }

    private static String getLibs() {
        StringBuilder sb = new StringBuilder();
        for (String jar : jars) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(mavenRepositoryPath + jar);
        }
        return sb.toString();
    }

    private static String getProjectSrcDirs() {
        return "/Users/xuwenzhen/Projects/fdd/common/house.graphql.cp.fdd/src/main/java:/Users/xuwenzhen/Projects/fdd/common/graphql-engine.cp.fdd/graphql-provider/src/main/java";
    }

}