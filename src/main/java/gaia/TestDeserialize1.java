package gaia;

import com.alibaba.fastjson.JSON;

public class TestDeserialize1 {
    public static void main(String[] args) {
        String s = null;

        s = "{\"@type\":\"gaia.Evil1\",\"cmd\":\"calc\"}";
//        JSON.parse(s).getClass();
//        JSON.parseObject(s).getClass();// parse和parseObject的区别，parseObject调用parse解析得到对象，然后JSON.toJSON 转json对象，这个过程中会调用getXxx isXxx 或反射取field值
//        JSON.parseObject(s, User.class);// 首先使用对应类反序列化器来反序列化，如果发现类不匹配，则指定目标类调用父类的反序列化com.alibaba.fastjson.parser.deserializer.ASMJavaBeanDeserializer.deserialze

//        s = "[{\"@type\":\"gaia.Evil1\",\"cmd\":\"calc\"}]";
//        JSON.parseArray(s);
//        JSON.parseArray(s, User.class);




    }
}
