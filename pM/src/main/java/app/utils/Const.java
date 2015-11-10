package app.utils;

import java.util.Map;

public class Const {

    public static String CURRENT_USER_ID = "";

    public static String CURRENT_ACCESS_TOKEN = "";

    /**Cache**/
    public static final String Cache_User_Id = "Cache_User_Id";

    public static final String Cache_Access_Token = "Cache_Access_Token";

    /**Handler Code**/
    public static final int Handler_Login_Success = 100001;

    public static String[] cityName =
            {
                    "北京市",
                    "天津市",
                    "上海市",
                    "重庆市",
                    "河北省",
                    "河南省",
                    "云南省",
                    "辽宁省",
                    "黑龙江省",
                    "湖南省",
                    "安徽省",
                    "山东省",
                    "新疆",
                    "江苏省",
                    "浙江省",
                    "江西省",
                    "湖北省",
                    "广西",
                    "甘肃省",
                    "山西省",
                    "内蒙古",
                    "陕西省",
                    "吉林省",
                    "福建省 ",
                    "贵州省 ",
                    "广东省",
                    "青海省",
                    "西藏",
                    "四川省",
                    "宁夏",
                    "海南省",
                    "台湾省",
                    "香港",
                    "澳门"
            };

    public static String[] airQuality = {
            "空气质量优", "空气质量良", "轻度污染", "中度污染", "重度污染", "严重污染"
    };

    public static String[] heathHint = {
            "适合户外活动", "易感人群减少户外活动", "适量减少户外活动", "避免户外活动"
    };

    public static String[] ringState = {
            "Bio3手环未配置", "Bio3手环已配置", "Bio3手环已连接"
    };

    public static String[] ringState2 = {
            "Bio3检测盒未连接", "Bio3检测盒已经连接"
    };

    public static String[] downloadPeriod = {
      "0:10","1:10","2:10","3:10","4:10","5:10","6:10","7:10","8:10",
       "9:10","10:10","11:10","12:10","13:10","14:10","15:10","16:10","17:10",
       "18:10","19:10","20:10","21:10","22:10","23:10"
    };
}