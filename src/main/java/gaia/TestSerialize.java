package gaia;

import com.alibaba.fastjson.JSON;

import java.util.HashSet;
import java.util.Set;

public class TestSerialize {
    public static void main(String[] args) {
        HashSet<String> set = new HashSet<String>();
        set.add("abc");
        set.add("bbc");
        set.add("cdc");
        System.out.println(JSON.toJSONString(set));


    }
}
