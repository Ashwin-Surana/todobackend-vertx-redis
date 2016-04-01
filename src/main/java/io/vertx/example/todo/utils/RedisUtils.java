package io.vertx.example.todo.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by ashwin on 19/03/16.
 */
public class RedisUtils {

    private interface  AsyncResultJsonObjectHandler extends Handler<AsyncResult<JsonObject>> {

    }

    /*
        A util function to retrieve hash for the given list of keys.
     */
    public static void getHashes(RedisClient client, List<Object> keys, Handler<JsonArray> onComplete){
        JsonArray result = new JsonArray();

        // we retrieve hashes by hgetall for each key recursively

        BiFunction<BiFunction, Integer, AsyncResultJsonObjectHandler> getAll = (get, index) -> hGet -> {
            result.add(hGet.result());
            if (index == keys.size()) {
                onComplete.handle(result);
            } else {                                        // RECURSION OCCURS HERE ----------v
                client.hgetall(keys.get(index).toString(), (AsyncResultJsonObjectHandler) get.apply(get, index + 1));
            }
        };

        if (keys == null || keys.size() <= 0) {
            onComplete.handle(result);
            return;
        }

        client.hgetall(keys.get(0).toString(), getAll.apply(getAll, 1));
    }
}
