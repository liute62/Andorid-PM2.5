package app.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class SharedPreferencesUtil {

	private static MySqlLiteHelper sqlLiteHelper = null;
	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "setting.db";
	private static final String TABLE_NAME = "setting_info";
	private static SQLiteDatabase db = null;
	
	private static class MySqlLiteHelper extends SQLiteOpenHelper {

		public MySqlLiteHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table if not exists " + TABLE_NAME + "("
					+ "key text primary key," + "value text," + "text1 text,"
					+ "text2 text," + "text3 text," + "text4 text,"
					+ "text5 text)");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}

	}
	
		
	private static Object getValue(Context context, String key, Class<?> clazz) {
		if (sqlLiteHelper == null) {
			sqlLiteHelper = new MySqlLiteHelper(context, DB_NAME, null,
					DB_VERSION);
			db = sqlLiteHelper.getWritableDatabase();
		}
		Object ret = null;
		if (clazz == Integer.class) {
			ret = -1;
		} else if (clazz == Long.class) {
			ret = -1L;
		} else if (clazz == Double.class) {
			ret = -1D;
		} else if (clazz == Float.class) {
			ret = -1F;
		} else if (clazz == String.class) {
			ret = null;
		}

		String sql = "select value from " + TABLE_NAME + " where key=?";
		try {
			Cursor cursor = db.rawQuery(sql, new String[] { key });
			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
				if (clazz == Integer.class) {
					ret = cursor.getInt(0);
				} else if (clazz == Float.class) {
					ret = cursor.getFloat(0);
				} else if (clazz == Long.class) {
					ret = cursor.getLong(0);
				} else if (clazz == Double.class) {
					ret = cursor.getDouble(0);
				} else if (clazz == Boolean.class) {
					ret = cursor.getString(0).equals("no") ? false : true;
				} else if (clazz == String.class) {
					ret = cursor.getString(0);
				}
			}
			cursor.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return ret;
	}
	
	private static Object getValue(Context context, String key, Class<?> clazz,
			Object defValue) {
		Object ret = getValue(context, key, clazz);

		if (clazz == Integer.class) {
			if ((Integer) ret == -1) {
				ret = defValue;
			}
		} else if (clazz == Long.class) {
			if ((Long) ret == -1l) {
				ret = defValue;
			}
		} else if (clazz == Double.class) {
			if ((Double) ret == -1D) {
				ret = defValue;
			}
		} else if (clazz == Float.class) {
			if ((Float) ret == -1F) {
				ret = defValue;
			}
		} else if (clazz == String.class) {
			if (ret == null) {
				ret = defValue;
			}
		} else if (clazz == Boolean.class) {
			if (ret == null) {
				ret = defValue;
			}
		}

		return ret;
	}
	
	public static boolean getBooleanValue(Context context, String key,
			boolean defValue) {
		return (Boolean) getValue(context, key, Boolean.class, defValue);
	}
	
	public static boolean getBooleanValue(Context context, String key) {
		return getBooleanValue(context, key, false);
	}
	
	
	
	
	
	private static void setValuePrivate(Context context, String key,
			Object value) {
		if (sqlLiteHelper == null) {
			sqlLiteHelper = new MySqlLiteHelper(
					context.getApplicationContext(), DB_NAME, null, DB_VERSION);
			db = sqlLiteHelper.getWritableDatabase();
		}

		try {
			String sql = "select count(*) from " + TABLE_NAME
					+ " where key = ?";
			if (value == null) {
				sql = "delete from " + TABLE_NAME + " where key = ?";
				db.execSQL(sql, new Object[] { key });
			} else {
				Cursor cursor = db.rawQuery(sql, new String[] { key });
				int count = 0;
				if (cursor.moveToFirst()) {
					count = cursor.getInt(0);
				}
				cursor.close();
				if (count != 0) {
					sql = "update " + TABLE_NAME + " set value=? where key=?";
					db.execSQL(sql, new Object[] { value, key });
				} else {
					sql = "insert into " + TABLE_NAME
							+ " (key, value) values (?,?)";
					db.execSQL(sql, new Object[] { key, value });
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
	
	public static void setValue(Context context, String key, boolean value) {
		if (value == true)
			setValuePrivate(context, key, "yes");
		else
			setValuePrivate(context, key, "no");
	}
}
