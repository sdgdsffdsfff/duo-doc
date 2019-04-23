package com.fangdd.tp.doclet.exporter.impl;

import com.alibaba.fastjson.JSONObject;
import com.fangdd.tp.doclet.DocletConfig;
import com.fangdd.tp.doclet.constant.DocletConstant;
import com.fangdd.tp.doclet.exporter.Exporter;
import com.fangdd.tp.doclet.helper.BookHelper;
import com.fangdd.tp.doclet.helper.GzipHelper;
import com.fangdd.tp.doclet.helper.HttpHelper;
import com.fangdd.tp.doclet.helper.Logger;
import com.fangdd.tp.doclet.pojo.Artifact;
import com.fangdd.tp.doclet.pojo.Chapter;
import com.fangdd.tp.doclet.pojo.DocDto;
import com.fangdd.tp.doclet.pojo.Entity;
import com.fangdd.tp.doclet.render.EntityHandle;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 导出到服务器
 *
 * @author ycoe
 * @date 18/1/21
 */
public class ServerExporter implements Exporter {
    private static final Logger logger = new Logger();

    @Override
    public String exporterName() {
        return "server";
    }

    @Override
    public boolean export(List<Chapter> chapterSet) {
        List<Entity> entitySet = Lists.newArrayList();

        for (Map.Entry<String, Entity> entry : EntityHandle.getEntityMap().entrySet()) {
            entitySet.add(entry.getValue());
        }

        DocDto request = new DocDto();
        Artifact artifact = BookHelper.getArtifact();
        request.setArtifact(artifact);
        request.setChapters(chapterSet);
        request.setEntities(entitySet);
        request.setDocletVersion(DocletConstant.DOCLET_VERTION);

        Map<String, String> markdownMap = BookHelper.getMarkdownDocMap();
        if (!markdownMap.isEmpty()) {
            request.setMarkdownMaps(markdownMap);
        }

        String docData = JSONObject.toJSONString(request);
        try {
            byte[] gzipData = GzipHelper.compress(docData);

            //POST to API
            String data = HttpHelper.post(DocletConfig.server + "/api/doc", gzipData);
            JSONObject json = JSONObject.parseObject(data);
            if (json == null) {
                logger.info("文档上传服务器失败！");
                //即使失败，也不去影响正常的进度
                return true;
            }
            String url = json.getString("data");
            if (Strings.isNullOrEmpty(url)) {
                logger.info("文档上传服务器失败！");
            } else {
                logger.info("Document Site: " + DocletConfig.server + url);
            }
        } catch (IOException e) {
            logger.error("将文档同步到服务器：" + DocletConfig.server + "失败！", e);
        }
        return true;
    }
}
