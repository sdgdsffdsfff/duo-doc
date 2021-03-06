package com.fangdd.tp.doclet.analyser.rest;

import com.fangdd.tp.doclet.DocletConfig;
import com.fangdd.tp.doclet.constant.DocletConstant;
import com.fangdd.tp.doclet.constant.EntityConstant;
import com.fangdd.tp.doclet.constant.SpringMvcConstant;
import com.fangdd.tp.doclet.enums.ApiTypeEnum;
import com.fangdd.tp.doclet.helper.*;
import com.fangdd.tp.doclet.pojo.Api;
import com.fangdd.tp.doclet.pojo.Chapter;
import com.fangdd.tp.doclet.pojo.EntityRef;
import com.fangdd.tp.doclet.pojo.Section;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Tag;

import java.util.List;

/**
 * @author xuwenzhen
 * @date 18/1/9
 */
public class RestFulAnalyser {
    private static final String URL_SPLITTER = "/";
    private static final String METHOD_POST = "POST";
    private static final String METHOD = "method";
    private static final String VALUE = "value";
    private static final String BATCH = "batch";
    private static final String FALSE = "false";
    private static final String ID_SPLITTER = "idSplitter";

    public static void analyse(ClassDoc classDoc) {
        //@RequestMapping
        AnnotationDesc requestMappingAnnotation = BookHelper.requestMappingAnnotation;
        AnnotationDesc.ElementValuePair[] vs = requestMappingAnnotation.elementValues();
        List<String> basePaths = RequestMappingAnnotationHelper.getRequestMappingAnnotationValues(vs, VALUE);
        if (basePaths.isEmpty()) {
            basePaths.add("");
        }

        Tag[] tags = classDoc.tags();
        String chapterName = TagHelper.getStringValue(tags, DocletConfig.tagChapter, DocletConstant.DEFAULT_CHAPTER_NAME);
        Chapter chapter = BookHelper.getChapter(StringHelper.firstLine(chapterName));

        Integer order = TagHelper.getIntegerValue(tags, DocletConfig.tagChapterSort);
        if (order != null) {
            chapter.setOrder(order);
        }
        String sectionName = TagHelper.getStringValue(tags, DocletConfig.tagSection, null);
        String comment = classDoc.commentText();
        if (StringHelper.isEmpty(sectionName)) {
            //尝试使用注释第一行
            if (StringHelper.isNotEmpty(comment)) {
                sectionName = StringHelper.firstLine(comment);
                comment = StringHelper.deleteFirstLine(comment);
            }
        }
        //类全名
        String classFullName = classDoc.qualifiedTypeName();
        if (StringHelper.isEmpty(sectionName)) {
            sectionName = classFullName;
        }

        Section section = BookHelper.getSections(chapter, sectionName);
        section.setCode(classFullName);

        Integer sectionOrder = TagHelper.getIntegerValue(tags, DocletConfig.tagSectionSort);
        if (sectionOrder != null) {
            section.setOrder(sectionOrder);
        }
        section.setComment(comment);

        String deprecated = TagHelper.getStringValue(tags, DocletConfig.tagDeprecated, null);

        MethodDoc[] methods = classDoc.methods();
        for (MethodDoc method : methods) {
            if (TagHelper.contendTag(method.tags(), DocletConfig.tagDisable)) {
                continue;
            }
            Api api = analyseApi(method, section, basePaths);
            if (api != null && !Strings.isNullOrEmpty(deprecated) && Strings.isNullOrEmpty(api.getDeprecated())) {
                api.setDeprecated(deprecated);
            }
        }
        if (section.getApis().isEmpty()) {
            // 如果api为空时，直接清掉
            BookHelper.delSections(chapter, section);
        }
        if (chapter.getSections().isEmpty()) {
            BookHelper.delChapter(chapter);
        }
    }

    private static Api analyseApi(MethodDoc methodDoc, Section section, List<String> basePaths) {
        List<String> methods = null;
        AnnotationDesc.ElementValuePair[] vs = null;

        for (String[] methodMap : DocHelper.METHOD_MAPPINGS) {
            AnnotationDesc requestMappingAnnotation = AnnotationHelper.getAnnotation(methodDoc.annotations(), methodMap[0]);
            if (requestMappingAnnotation != null) {
                vs = requestMappingAnnotation.elementValues();
                if (Strings.isNullOrEmpty(methodMap[1])) {
                    methods = RequestMappingAnnotationHelper.getRequestMappingAnnotationValues(vs, METHOD);
                } else {
                    methods = Lists.newArrayList(methodMap[1]);
                }
                break;
            }
        }

        if (vs == null) {
            return null;
        }
        List<String> paths = RequestMappingAnnotationHelper.getRequestMappingAnnotationValues(vs, VALUE);
        if (paths.isEmpty()) {
            paths.add("");
        }
        List<String> apiPaths = Lists.newArrayList();
        for (String basePath : basePaths) {
            for (String path : paths) {
                String apiPath;
                if (basePath.endsWith(URL_SPLITTER)) {
                    if (path.startsWith(URL_SPLITTER)) {
                        apiPath = basePath + path.substring(1);
                    } else {
                        apiPath = basePath + path;
                    }
                } else {
                    if (path.startsWith(URL_SPLITTER)) {
                        apiPath = basePath + path;
                    } else {
                        apiPath = basePath + URL_SPLITTER + path;
                    }
                }
                if (apiPath.length() > 0 && !apiPath.startsWith(URL_SPLITTER)) {
                    apiPath = URL_SPLITTER + apiPath;
                }
                if (!apiPaths.contains(apiPath)) {
                    apiPaths.add(apiPath);
                }
            }
        }

        Api api = BaseApiInfoHelper.getApiBase(methodDoc, section);

        setGraphqlInfo(methodDoc, api);

        api.setPaths(apiPaths);
        api.setType(ApiTypeEnum.RESTFUL.getType());
        if (methods != null && !methods.isEmpty()) {
            api.setMethods(methods);
        }
        //如果是RestFul接口，且参数有@RequestBody时，强制为 POST ??
        boolean isMethodEmpty = api.getMethods() == null || api.getMethods().isEmpty();
        if (hasRequestBodyParam(api) && isMethodEmpty) {
            api.setMethods(Lists.newArrayList(METHOD_POST));
        }
        return api;
    }

    private static void setGraphqlInfo(MethodDoc methodDoc, Api api) {
        AnnotationDesc graphqlJsonAnnotation = AnnotationHelper.getAnnotation(
                methodDoc.annotations(), EntityConstant.ANNOTATION_GRAPHQL_JSON
        );
        if (graphqlJsonAnnotation != null) {
            //标注了@GraphqlJson
            api.setGraphqlJson(true);
        }

        AnnotationDesc graphqlProviderAnnotation = AnnotationHelper.getAnnotation(
                methodDoc.annotations(), EntityConstant.ANNOTATION_GRAPHQL_PROVIDER
        );
        if (graphqlProviderAnnotation == null) {
            return;
        }
        String providerName = AnnotationHelper.getStringValue(graphqlProviderAnnotation, VALUE);
        if (Strings.isNullOrEmpty(providerName)) {
            return;
        }
        String batchProvier = AnnotationHelper.getStringValue(graphqlProviderAnnotation, BATCH, FALSE);
        api.setProviderName(providerName);
        boolean batchProvider = Boolean.parseBoolean(batchProvier);
        api.setBatchProvider(batchProvider);
        if (batchProvider) {
            String idSplitter = AnnotationHelper.getStringValue(graphqlProviderAnnotation, ID_SPLITTER, ",");
            api.setIdSplitter(idSplitter);
        }
    }

    private static boolean hasRequestBodyParam(Api api) {
        List<EntityRef> params = api.getRequestParams();
        if (params == null || params.isEmpty()) {
            return false;
        }
        for (EntityRef er : params) {
            if (SpringMvcConstant.ANNOTATION_REQUEST_BODY.equals(er.getAnnotation())) {
                return true;
            }
        }
        return false;
    }
}
