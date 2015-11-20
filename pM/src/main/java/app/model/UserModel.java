package app.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class UserModel extends BaseModel {

    public static final String ID = "id";

    public static final String NAME = "name";

    public static final String PASSWORD = "password";

    public static final String EMAIL = "email";

    public static final String FIRSTNAME = "firstname";

    public static final String LASTNAME = "lastname";

    public static final String SEX = "sex";

    public static final String PHONE = "phone";

    private String id;

    private String name;

    private String password;

    private String email;

    private String firstname;

    private String lastname;

    private String sex;

    private String phone;

    public static UserModel AddDataForTest() {
        UserModel userModel = new UserModel();
        userModel.setName("usermodel_name");
        userModel.setPassword("usermodel_password");
        userModel.setEmail("usermodel_email");
        userModel.setFirstname("usermodel_firstname");
        userModel.setLastname("usermodel_lastname");
        userModel.setSex("usermodel_sex");
        userModel.setPhone("usermodel_phone");
        return userModel;
    }

    public static UserModel parse(JSONObject object) throws JSONException {
        UserModel bean = new UserModel();
        if (object.has(ID)) {
            bean.setId(object.getString(ID));
        }
        if (object.has(NAME)) {
            bean.setName(object.getString(NAME));
        }
        if (object.has(PASSWORD)) {
            bean.setPassword(object.getString(PASSWORD));
        }
        if (object.has(EMAIL)) {
            bean.setEmail(object.getString(EMAIL));
        }
        if (object.has(FIRSTNAME)) {
            bean.setFirstname(object.getString(FIRSTNAME));
        }
        if (object.has(LASTNAME)) {
            bean.setLastname(object.getString(LASTNAME));
        }
        if (object.has(SEX)) {
            bean.setSex(object.getString(SEX));
        }
        if (object.has(PHONE)) {
            bean.setPhone(object.getString(PHONE));
        }
        return bean;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(ID, getId());
        if (name != null) {
            object.put(NAME, getName());
        }
        if (password != null) {
            object.put(PASSWORD, getPassword());
        }
        if (email != null) {
            object.put(EMAIL, getEmail());
        }
        if (firstname != null) {
            object.put(FIRSTNAME, getFirstname());
        }
        if (lastname != null) {
            object.put(LASTNAME, getLastname());
        }
        if (sex != null) {
            object.put(SEX, getSex());
        }
        if (phone != null) {
            object.put(PHONE, getPhone());
        }
        return object;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
