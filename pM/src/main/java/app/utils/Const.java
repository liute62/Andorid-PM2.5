package app.utils;

import app.model.PMModel;

public class Const {

    public static String CURRENT_USER_ID = "";

    public static String CURRENT_ACCESS_TOKEN = "";

    public static double CURRENT_LONGITUDE = 0;

    public static double CURRENT_LATITUDE = 0;

    public static PMModel CURRENT_PM_MODEL;

    public static boolean CURRENT_INDOOR = false;

    public static int CURRENT_CHART1_INDEX = 1;

    public static int CURRENT_CHART2_INDEX = 2;

    public static MotionStatus CURRENT_STATUS;

    public static Double CURRENT_VENTILATION_VOLUME = 0.0;

    public static int CURRENT_STEPS_NUM = 0;

    public static boolean CURRENT_DB_RUNNING = false;

    /**Cache**/
    public static final String Cache_App_Initialized = "Cache_App_Initialized";

    public static final String Cache_User_Id = "Cache_User_Id";

    public static final String Cache_Access_Token = "Cache_Access_Token";

    public static final String Cache_PM_State = "Cache_PM_State";

    public static final String Cache_Longitude = "Cache_Longitude";

    public static final String Cache_Latitude = "Cache_Latitude";

    /**Handler Code**/
    public static final int Handler_Login_Success = 100001;

    public static final int Handler_PM_Data = 100002;

    /**Intent Tag Code**/
    public static final String Intent_PM_Density = "Intent_PM_Density";

    public static final String Intent_DB_PM_Result = "Intent_DB_PM_Result";

    public static final String Intent_DB_PM_TIME = "Intent_DB_PM_TIME";

    public static final String Intent_DB_Ven_Volume = "Intent_DB_Ven_Volume";

    public static final String Intent_Main_Location = "Intent_Main_Location";

    /**Service & Activity Code**/
    public static final String Action_DB_MAIN_PMResult = "Action_DB_MAIN_PMResult";

    public static final String Action_DB_MAIN_PMDensity = "Action_DB_MAIN_PMDensity";


    /**GPS**/
    public static int LOCATION_TIME_INTERVAL = 60 ; //1MIN

    public static final String APP_MAP_KEY = "";

    /**Time related values**/
    public final static int DENSITY_TIME_INTERVAL = 60*60*1000; //1Hour

    public final static int DB_PM_Search_INTERVAL = 1000 * 5;

    public final static int DB_Location_INTERVAL = 1000 * 5;

    public static final String ERROR_NO_GPS = "请先打开定位！";

    public static final String ERROR_NO_PM_DATA = "本地暂无PM数据!";

    public static final String ERROR_NO_CITY_RESULT = "获取当前城市失败";

    //breath according to state
    public static final double boy_breath = 6.6; // L/min
    public static final double girl_breath = 6.0; // L/min
    public static double static_breath = boy_breath;
    public static final double walk_breath = 2.1 * static_breath;
    public static final double bicycle_breath = 2.1 * static_breath;
    public static final double run_breath = 6 * static_breath;

    /**Movement**/
    public static enum MotionStatus{
        NULL, STATIC, WALK, RUN
    }

    /****/
    public static final String Info_No_Initial = "系统检测到本机并无数据，请确保网络和GPS正常，按确认键进行初始化。";

    public static final String Info_Turn_Off_Service = "关闭数据上传服务";

    public static final String Info_Turn_On_Service = "开启数据上传服务";

    /**For Chart**/
    public static String[] Chart1_X = new String[] {
            "1点","2点","3点","4点","5点","6点",
            "7点","8点","9点","10点","11点","12点",
            "13点","14点","15点","16点","17点","18点",
            "19点","20点","21点","22点","23点","24点"
    };

    public static String[] Chart1_2_X = new String[]{
            "1天","2天","3天","4天","5天","6天","7天"
    };

    public static String[] Chart_title = new String[]
    {
        "","单位时间吸入的PM2.5的量(L)","单位时间平均暴露浓度(ug/m3)","累积吸入的PM2.5量(L)","平均单位时间吸入的空气量(L/min)","","累积吸入的空气量(L)",
    };

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