package gaia;

import com.alibaba.fastjson.JSON;

public class TestDeserialize {
    public static void main(String[] args) {
        String s = null;

        s = "{\"@type\":\"gaia.Evil\",\"cmd\":\"calc\"}";
        JSON.parse(s).getClass();
        JSON.parseObject(s).getClass();
        JSON.parseObject(s, User.class);

        s = "[{\"@type\":\"gaia.Evil\",\"cmd\":\"calc\"}]";
        JSON.parseArray(s);
        JSON.parseArray(s, User.class);




    }
}
