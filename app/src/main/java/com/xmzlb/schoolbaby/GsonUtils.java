package com.xmzlb.schoolbaby;

import com.google.gson.Gson;

/**
 * Created by zyz on 2016/5/11 0011.
 * QQ:344100167
 */
public final class GsonUtils {
    public static <T> T parseJSON(String json, Class<T> clazz) {
        Gson gson = new Gson();
        T info = gson.fromJson(json, clazz);
        return info;
    }

    private GsonUtils(){}
}
