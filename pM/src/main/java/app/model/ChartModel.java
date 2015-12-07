package app.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Administrator on 11/25/2015.
 */
public class ChartModel extends BaseModel {

    private Map data;

    public ChartModel() {
    }

    public static JSONObject toJSONObject(ChartModel model) {
        JSONObject object = new JSONObject();
        Map map = model.getData();
        return object;
    }

    public static JSONObject toJSONObject(Map map) {
        JSONObject object = new JSONObject();
        for (Object key : map.keySet()) {
            //default Key: Set<Integer> Value: Float
            Integer tmpkey = (Integer) key;
            try {
                object.put(String.valueOf(tmpkey), map.get(tmpkey));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return object;
    }

    public static ChartModel Parse(JSONObject object) {
        ChartModel model = new ChartModel();
        Map<Integer, Float> map = new HashMap<>(); // we just need this type
        Iterator<String> iterator = object.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            try {
                Object value = object.get(key);
                map.put(Integer.valueOf(key), (Float) value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        model.setData(map);
        return model;
    }

    public Map getData() {
        return data;
    }

    public void setData(Map data) {
        this.data = data;
    }
}
