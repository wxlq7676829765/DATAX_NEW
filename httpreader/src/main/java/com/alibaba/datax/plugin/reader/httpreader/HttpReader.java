package com.alibaba.datax.plugin.reader.httpreader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.unstructuredstorage.reader.UnstructuredStorageReaderUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class HttpReader extends Reader {
    public static class Job extends Reader.Job {
        private static final Logger LOG = LoggerFactory.getLogger(Job.class);

        private Configuration originConfig = null;
        private String url;
        private String action;
        @Override
        public void init() {
            this.originConfig = this.getPluginJobConf();
        }

        @Override
        public void prepare() {
        }

        @Override
        public List<Configuration> split(int adviceNumber) {
            LOG.debug("split() begin...");
            List<Configuration> readerSplitConfigs = new ArrayList<Configuration>();
            int splitNumber = 1;
            Configuration splitedConfig = this.originConfig.clone();
            readerSplitConfigs.add(splitedConfig);
            LOG.debug("split() ok and end...");
            return readerSplitConfigs;

        }

        @Override
        public void post() {
        }

        @Override
        public void destroy() {
        }

    }

    public static class Task extends Reader.Task {
        private static Logger LOG = LoggerFactory.getLogger(Task.class);

        private Configuration readerSliceConfig = null;
        private String url;
        private String action;
        private String header;
        private String params;
        private String datanode;
        private String content_type;
        @Override
        public void init() {

        }

        @Override
        public void prepare() {
        }

        @Override
        public void startRead(RecordSender recordSender) {
            this.readerSliceConfig = this.getPluginJobConf();
            this.url = readerSliceConfig.getString(Key.URL);
            this.action = readerSliceConfig.getString(Key.ACTION);
            this.params = readerSliceConfig.getString(Key.PARAMS);
            this.datanode = readerSliceConfig.getString(Key.DATANODE);
            this.content_type = readerSliceConfig.getString(Key.CONTENT_TYPE);
            LOG.info(url+"_"+action+"_"+params+"_"+datanode+"_"+content_type);
            String ret = null;
            if ("get".equals(this.action)){
                if(!this.params.isEmpty()){
                    this.url=this.url+"?"+params;
                }
                 ret = HttpUtils.get(this.url);
            }else if("post".equals(this.action)){
                Map<String, Object> map = new HashMap<>();
                if("form".equals(content_type)){
                    if(!this.params.isEmpty()){
                        String[] split = params.split("&");
                        for (String s : split) {
                            String[] pair = s.split("=");
                            map.put(pair[0],pair[1]);
                        }
                    }
                    ret = HttpUtils.sendFromPost(this.url, map);
                }else if("json".equals(content_type)){
                    JSONObject jsonObject = JSONObject.parseObject(params);
                    ret = HttpUtils.sendJsonPost(this.url,jsonObject);
                }
            }
            // json.data
            String[] split = datanode.split("\\.");
            LOG.info("开始json解析----------------------------！！！");
            JSONObject jsonObj = JSONObject.parseObject(ret,Feature.OrderedField);

            JSONObject jsonObj1 = jsonObj;
            JSONArray jsonArray = null;
            for (int i = 0; i < split.length; i++) {
                if(i< (split.length-1)){
                     jsonObj1 = JSONObject.parseObject(jsonObj1.get(split[i]).toString());
                }else {
                    try {
                        jsonArray = (JSONArray) jsonObj1.get(split[i].toString());
                    }catch (Exception e){
                        jsonArray = JSONArray.parseArray(jsonObj1.get(split[i]).toString());
                    }
                }
            }
            UnstructuredStorageReaderUtil.readFromJSONArray(jsonArray,readerSliceConfig,recordSender,this.getTaskPluginCollector());
            recordSender.flush();
        }

        @Override
        public void post() {
        }

        @Override
        public void destroy() {
        }
        
        
        
    }
}
