package app.utils;

import app.model.PMModel;

public class Const {

    public static String Name_DB_Service = "app.services.DBService";

    public static String CURRENT_USER_ID = "0";

    public static String CURRENT_ACCESS_TOKEN = "-1";

    public static String CURRENT_USER_NAME = "-1";

    public static String CURRENT_USER_NICKNAME = "-1";

    public static String CURRENT_USER_GENDER = "-1";

    public static double Lasttime_LONGITUDE = 121.41;

    public static double Lasttime_LATITUDE = 31.14; //default is shanghai

    public static PMModel CURRENT_PM_MODEL;

    public static boolean CURRENT_INDOOR = false;

    public static int CURRENT_CHART1_INDEX = 1;

    public static int CURRENT_CHART2_INDEX = 2;

    public static MotionStatus CURRENT_STATUS;

    public static Double CURRENT_VENTILATION_VOLUME = 0.0;

    public static int CURRENT_STEPS_NUM = 0;

    public static boolean CURRENT_DB_RUNNING = false;

    /**
     * Cache*
     */
    public static final String Cache_App_Initialized = "Cache_App_Initialized";

    public static final String Cache_User_Id = "Cache_User_Id";
    public static final String Cache_User_Name = "Cache_User_Name";
    public static final String Cache_User_Nickname = "Cache_User_Nickname";
    public static final String Cache_User_Gender = "Cache_User_Gender";
    public static final String Cache_Access_Token = "Cache_Access_Token";

    public static final String Cache_PM_Density = "Cache_PM_Density";
    public static final String Cache_PM_LastHour = "Cache_PM_LastHour";
    public static final String Cache_PM_LastDay = "Cache_PM_LastDay";
    public static final String Cache_PM_LastWeek = "Cache_PM_LastWeek";

    public static final String Cache_Longitude = "Cache_Longitude";
    public static final String Cache_Latitude = "Cache_Latitude";
    public static final String Cache_City = "Cache_City";

    public static final String Cache_Chart_1 = "Cache_Chart_1";
    public static final String Cache_Chart_2 = "Cache_Chart_2";
    public static final String Cache_Chart_3 = "Cache_Chart_3";
    public static final String Cache_Chart_4 = "Cache_Chart_4";
    public static final String Cache_Chart_5 = "Cache_Chart_5";
    public static final String Cache_Chart_6 = "Cache_Chart_6";
    public static final String Cache_Chart_7 = "Cache_Chart_7";
    public static final String Cache_Chart_7_Date = "Cache_Chart_7_Date";
    public static final String Cache_Chart_8 = "Cache_Chart_8";
    public static final String Cache_Chart_10 = "Cache_Chart_10";
    public static final String Cache_Chart_12 = "Cache_Chart_12";
    public static final String Cache_Chart_12_Date = "Cache_Chart_12_Date";


    /**
     * Handler Code*
     */
    public static final int Handler_Login_Success = 100001;

    public static final int Handler_PM_Density = 100002;

    public static final int Handler_PM_Data = 100003;

    public static final int Handler_Modify_Pwd_Success = 100004;

    /**
     * Intent Tag Code*
     */
    public static final String Intent_PM_Id = "Intent_PM_Id";

    public static final String Intent_PM_Density = "Intent_PM_Density";

    public static final String Intent_DB_PM_Day = "Intent_DB_PM_Day";

    public static final String Intent_DB_PM_Hour = "Intent_DB_PM_Hour";

    public static final String Intent_DB_PM_TIME = "Intent_DB_PM_TIME";

    public static final String Intent_DB_Ven_Volume = "Intent_DB_Ven_Volume";

    public static final String Intent_DB_PM_Week = "Intent_DB_PM_Week";

    public static final String Intent_DB_PM_Chart1 = "Intent_DB_PM_Chart1";

    public static final String Intent_Main_Location = "Intent_Main_Location";

    public static final String Intent_chart1_data = "Intent_chart1_data";

    public static final String Intent_chart2_data = "Intent_chart2_data";

    public static final String Intent_chart3_data = "Intent_chart3_data";

    public static final String Intent_chart4_data = "Intent_chart4_data";

    public static final String Intent_chart5_data = "Intent_chart5_data";

    public static final String Intent_chart6_data = "Intent_chart6_data";

    public static final String Intent_chart7_data = "Intent_chart7_data";

    public static final String Intent_chart_7_data_date = "Intent_chart_7_data_date";

    public static final String Intent_chart8_data = "Intent_chart8_data";

    public static final String Intent_chart10_data = "Intent_chart10_data";

    public static final String Intent_chart12_data = "Intent_chart12_data";

    public static final String Intent_chart_12_data_date = "Intent_chart_12_data_date";

    /**
     * Service & Activity Code*
     */
    public static final String Action_DB_MAIN_PMResult = "Action_DB_MAIN_PMResult";

    public static final String Action_DB_MAIN_PMDensity = "Action_DB_MAIN_PMDensity";

    public static final String Action_Chart_Cache = "Action_Chart_Cache";

    public static final String Action_Chart_Result_1 = "Action_Chart_Result_1";

    public static final String Action_Chart_Result_2 = "Action_Chart_Result_2";

    public static final String Action_Chart_Result_3 = "Action_Chart_Result_3";

    public static final int Action_Profile_Register = 200001;

    /**
     * GPS*
     */
    public static final String APP_MAP_KEY = "";

    /**
     * Time related values*
     */
    public final static int DB_PM_Search_INTERVAL = 1000 * 60 * 60;//1Hour

    public final static int DB_Location_INTERVAL = 1000 * 5;

    public final static int DB_PM_Cal_INTERVAL = 1000 * 5; //1min

    public static final String ERROR_NO_CITY_RESULT = "获取当前城市失败";

    public static final String ERROR_REGISTER_WRONG = "无法注册,请检查当前网络状态";
    //breath according to state
    public static final double boy_breath = 6.6; // L/min
    public static final double girl_breath = 6.0; // L/min
    public static double static_breath = boy_breath;
    public static final double walk_breath = 2.1 * static_breath;
    public static final double bicycle_breath = 2.1 * static_breath;
    public static final double run_breath = 6 * static_breath;

    /**
     * Movement*
     */
    public static enum MotionStatus {
        NULL, STATIC, WALK, RUN
    }

    /****/
    public static final String Info_No_Network = "无法连接服务器，请检查网络设置";

    public static final String Info_No_Initial = "系统检测到本机并无数据，请确保网络和GPS正常，按确认键进行初始化。";

    public static final String Info_Turn_Off_Service = "关闭后台服务";

    public static final String Info_Turn_On_Service = "开启后台服务";

    public static final String Info_Turn_Off_Upload = "关闭数据上传";

    public static final String Info_Turn_On_Upload = "开启数据上传";

    public static final String Info_Register_Success = "注册用户成功";

    public static final String Info_Register_Failed = "注册用户失败";

    public static final String Info_Register_pwdError = "两次输入的密码不一致";

    public static final String Info_Register_InputEmpty = "请填入必要信息";

    public static final String Info_Login_Success = "登录成功";

    public static final String Info_Login_Failed = "登录失败";

    public static final String Info_Login_Empty = "用户名或密码为空";

    public static final String Info_Login_Short = "用户名或密码长度过短";

    public static final String Info_Login_Space = "用户名或密码中有空格";

    public static final String Info_Login_First = "请先登录";

    public static final String Info_GPS_Open = "定位服务已打开!";

    public static final String Info_GPS_Turnoff = "请先打开定位!";

    public static final String Info_PMDATA_Failed = "获取PM2.5数据失败";

    public static final String Info_Modify_Pwd_Success = "修改密码成功";

    public static final String Info_Modify_Pwd_Error = "用户不存在或验证失败";

    public static final String Info_Reset_Confirm = "是否发送重置密码邮件？";

    public static final String Info_Reset_Success = "发送重置密码邮件成功";

    public static final String Info_Reset_Username_Fail = "缺少用户名参数";

    public static final String Info_Reset_NoUser_Fail = "用户名不存在";

    public static final String Info_Reset_Unknown_Fail = "发送重置密码邮件失败";

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
            "0:10", "1:10", "2:10", "3:10", "4:10", "5:10", "6:10", "7:10", "8:10",
            "9:10", "10:10", "11:10", "12:10", "13:10", "14:10", "15:10", "16:10", "17:10",
            "18:10", "19:10", "20:10", "21:10", "22:10", "23:10"
    };
}