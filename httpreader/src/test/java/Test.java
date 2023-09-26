import com.alibaba.fastjson.JSONArray;

import java.util.HashMap;
import java.util.Map;

public class Test {
    public static void main(String[] args) {
//        String params = "[{\"col1\":0,\"col2\":0},{\"col1\":1,\"col2\":10},{\"col1\":2,\"col2\":20},{\"col1\":3,\"col2\":30},{\"col1\":4,\"col2\":40}]";
//        String[] split = params.split("&");
//        Map<String, Object> map = new HashMap<>();
//
//        for (String s : split) {
//            String[] pair = s.split("=");
//            map.put(pair[0],pair[1]);
//        }
        String str =  "{\"data\":[{\"col1\":0,\"col2\":0},{\"col1\":1,\"col2\":10},{\"col1\":2,\"col2\":20},{\"col1\":3,\"col2\":30},{\"col1\":4,\"col2\":40}]}";
        JSONArray objects = JSONArray.parseArray(str);
        System.out.println(objects);
    }
}
