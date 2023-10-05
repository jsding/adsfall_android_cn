package com.ivy.build;

import com.ivy.tools.AppendUnknownFiles;
import com.ivy.tools.Base64;
import com.ivy.tools.CommonUtil;
import com.ivy.tools.CopySdk;
import com.ivy.tools.HttpRequest;
import com.ivy.tools.ImageProcessor;
import com.ivy.tools.OSInfo;
import com.ivy.tools.ProcessAssets;
import com.ivy.tools.ProcessSmaliAndXML;
import com.ivy.tools.ReplacePackage;
import com.json.JsonArray;
import com.json.JsonObject;
import com.json.JsonValue;
import com.json.WriterConfig;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

/* loaded from: EncryptTool.jar:com/ivy/build/ProcessBuild.class */
public final class ProcessBuild {
    private static final String PARAMS_GET_APPID = "package=%s&name=%s&type=%d";
    private static final String PARAMS_PUSH_DEFAULT_JSON = "appid=%s&json=%s";
    private static final String SHA_CMD = "keytool -exportcert -keystore %s -alias %s -storepass %s -keypass %s | openssl sha1 -binary | openssl base64";
    private static PrintWriter _infolog;
    private static PrintWriter _errlog;
    private static String rootPath;
    private static JsonObject config;
    private static String domain;
    static final int MAX_PROC_NUM = 3;
    private static final String[] TYPES = {"games", "apps", "aso"};
    private static final String[] COPY_FOLDERS = {"lib", "res", "smali", "assets"};
    static final Namespace namespace = new Namespace("android", "http://schemas.android.com/apk/res/android");

    private static void initLogs(File src) {
        try {
            if (_infolog != null) {
                _infolog.close();
            }
            if (_errlog != null) {
                _errlog.close();
            }
            String path = src.getAbsolutePath();
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            _infolog = new PrintWriter(path + "/info.log", "utf-8");
            _infolog.write("");
            _errlog = new PrintWriter(path + "/err.log", "utf-8");
            _errlog.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getType(String type) {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(type)) {
                return i + 1;
            }
        }
        return 1;
    }

    private static int requestAppId(JsonObject conf, String pkg, String name, int type) throws Exception {
        JsonObject data;
        JsonValue value = conf.get("appid");
        int id = 0;
        if (value != null && value.isNumber()) {
            id = value.asInt();
        }
        String url = domain + "/api/newapp?" + String.format("package=%s&name=%s&type=%d", pkg, URLEncoder.encode(name), Integer.valueOf(type));
        String result = HttpRequest.get(url);
        log("[Request AppId] : " + result);
        JsonObject tmp = JsonObject.readFrom(result);
        if (tmp.getInt("status", 0) == 1 && (data = tmp.get("data").asObject()) != null) {
            id = data.getInt("id", -1);
        }
        conf.set("appid", id);
        return id;
    }

    private static void sendDefaultJson(int appid, String json) throws Exception {
        String param = String.format("appid=%s&json=%s", Integer.valueOf(appid), Base64.encode(json));
        String url = domain + "/api/json";
        HttpRequest.postAsync(url, param, new HttpRequest.HttpResult() { // from class: com.ivy.build.ProcessBuild.1
            @Override // com.ivy.tools.HttpRequest.HttpResult
            public void onSuccess(String data) {
                ProcessBuild.log("===>Send default.json to server success : " + data);
            }

            @Override // com.ivy.tools.HttpRequest.HttpResult
            public void onFailure(String msg) {
                ProcessBuild.logErr("===>Send default.json to server success : " + msg);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void log(String msg) {
        if (_infolog != null && msg != null) {
            System.out.println(msg);
            _infolog.append((CharSequence) msg);
            _infolog.flush();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void logErr(String msg) {
        if (_errlog != null && msg != null) {
            System.out.println(msg);
            _errlog.append((CharSequence) msg);
            _errlog.flush();
        }
    }

    private static boolean checkLastProcessRunning() {
        StringBuffer sb = CommonUtil.doShell("ps aux | grep -v grep | grep java | grep ProcessBuild");
        if (sb != null && sb.toString().split("\n").length > 2) {
            System.out.println(sb.toString());
            return true;
        }
        return false;
    }

    private static void changeName(String target, String name, String appNameTag, String lang) throws Exception {
        if (name != null) {
            File res = new File(target + "/res/");
            if (res.isDirectory()) {
                File[] lst = res.listFiles();
                String pattern = String.format("string[@name=\"%s\"]", appNameTag);
                for (File d : lst) {
                    if (d.isDirectory() && d.getName().startsWith("values") && (lang == null || d.getName().contains(lang))) {
                        File[] lst2 = d.listFiles();
                        for (File f : lst2) {
                            Document xml = CommonUtil.readXML(f);
                            Element node = (Element) xml.getRootElement().selectSingleNode(pattern);
                            if (node != null) {
                                String originName = node.getText();
                                if (originName.length() > 0) {
                                    node.setText(CommonUtil.toEscaped(name));
                                }
                                System.out.println("===> Replace name from: <" + originName + "> to <" + name + "> in <" + f.getAbsolutePath() + ">");
                                CommonUtil.writeXML(f, xml);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Document readAndroidManifext(String target) {
        return CommonUtil.readXML(new File(target + "/AndroidManifest.xml"));
    }

    private static void writeAndroidManifext(String target, Document xml) throws Exception {
        CommonUtil.writeXML(new File(target + "/AndroidManifest.xml"), xml);
    }

    private static JsonObject prepare(String rootPath2, String srcPath, String target, String pkg, int type) throws Exception {
        String versionName;
        File appjson = new File(srcPath + "/app.json");
        String appConf = CommonUtil.readFile(appjson, (String) null);
        if (appConf != null) {
            JsonObject conf = JsonObject.readFrom(appConf);
            String name = conf.getString("name", null);
            int appid = requestAppId(conf, pkg, name, type);
            if (appid < 0) {
                throw new Exception("Can't get appid from : package=%s&name=%s&type=%d, with pkg = " + pkg + ", name = " + name + ", type = " + type);
            }
            int versionCode = conf.getInt("versionCode", 1);
            String versionName2 = conf.getString("versionName", "1.0.0");
            int minSdkVersion = conf.getInt("minSdkVersion", 8);
            int targetSdkVersion = conf.getInt("targetSdkVersion", 19);
            int versionCode2 = versionCode + 1;
            if (versionCode2 == 1) {
                versionName = "1.0.0";
            } else {
                int value = Integer.parseInt(versionName2.replaceAll("\\.", "")) + 1;
                versionName = ((value / 100) % 10) + "." + ((value / 10) % 10) + "." + (value % 10);
            }
            String originVersionCode = CommonUtil.getYmlInfo(new File(target + "/apktool.yml"), "versionCode");
            JsonObject ymlData = new JsonObject();
            ymlData.add("versionCode", versionCode2);
            ymlData.add("versionName", versionName);
            ymlData.add("minSdkVersion", minSdkVersion);
            ymlData.add("targetSdkVersion", targetSdkVersion);
            conf.set("versionCode", versionCode2);
            conf.set("versionName", versionName);
            CommonUtil.modifyYmlByJsonObject(target + "/apktool.yml", ymlData);
            JsonValue data = conf.get("deleteAssets");
            if (data != null) {
                JsonArray arr = data.asArray();
                if (arr.size() > 0) {
                    for (int i = 0; i < arr.size(); i++) {
                        CommonUtil.delete(target + "/assets/" + arr.get(i).asString());
                    }
                }
            }
            CommonUtil.prepareResFolder(target + "/res");
            String sdkVersion = conf.getString("sdk_version", "1.0");
            String sdkPath = rootPath2 + "/tools/sdk/" + sdkVersion;
            String sdkType = conf.getString("sdk_type", "FL_ADS");
            String defaultJson = null;
            File f = new File(srcPath + "/default.json");
            if (f.exists()) {
                defaultJson = CommonUtil.readFile(f, "{}");
            }
            Document xml = readAndroidManifext(target);
            String appNameTag = xml.getRootElement().element("application").attributeValue(new QName("label", CommonUtil.namespace));
            String appNameTag2 = appNameTag.substring(appNameTag.indexOf("/") + 1);
            String iconName = CommonUtil.getIconName(xml);
            String originPackage = CommonUtil.readPackage(xml);
            if (sdkVersion.equals("1.0")) {
                String tdType = conf.getString("td_type", "app");
                CopySdk.copy(sdkPath + "/" + sdkType, target);
                if (tdType.equals("game")) {
                    CopySdk.copy(sdkPath + "/TD/" + tdType, target);
                }
            } else {
                if (!new File(target + "/smali/android/support").isDirectory()) {
                    CommonUtil.copy(sdkPath + "/android", target, false);
                }
                String[] tmp = sdkType.split(",");
                for (int i2 = 0; i2 < tmp.length; i2++) {
                    if (tmp[i2].length() > 0) {
                        CopySdk.copy(sdkPath + "/" + tmp[i2], target);
                    }
                }
                new File(target + "/assets/" + CommonUtil.md5("config_" + originPackage + originVersionCode)).delete();
                if (defaultJson != null) {
                    JsonObject defaultJsonObj = JsonObject.readFrom(defaultJson);
                    defaultJsonObj.getInt("appid", -1);
                    defaultJsonObj.set("appid", appid);
                    defaultJson = defaultJsonObj.toString(WriterConfig.PRETTY_PRINT);
                    CommonUtil.writeFile(f, defaultJson);
                    CommonUtil.encrypt(f, new File(target + "/assets/" + CommonUtil.md5("config_" + pkg + versionCode2)));
                    CommonUtil.delete(target + "/assets/raw/default.json");
                    CommonUtil.delete(target + "/assets/raw/pt");
                    CommonUtil.delete(target + "/assets/raw/cf");
                }
            }
            sendDefaultJson(appid, defaultJson);
            for (int i3 = 0; i3 < COPY_FOLDERS.length; i3++) {
                CommonUtil.copy(srcPath + "/" + COPY_FOLDERS[i3], target + "/" + COPY_FOLDERS[i3], false);
            }
            if (new File(srcPath + "/replace/").isDirectory()) {
                CopySdk.copy(srcPath + "/replace/", target);
            }
            ImageProcessor.processIcon(srcPath + "/icon.png", target + "/res", iconName);
            ReplacePackage.replacePackage(target, pkg);
            changeName(target, name, appNameTag2, null);
            JsonValue names = conf.get("names");
            if (names != null) {
                JsonObject obj = names.asObject();
                List<String> langs = obj.names();
                for (String l : langs) {
                    String n = obj.getString(l, name);
                    changeName(target, n, appNameTag2, l);
                }
            }
            AppendUnknownFiles.appendUnknownFiles(target);
            JsonValue data2 = conf.get("encryptAssets");
            JsonValue data3 = data2 == null ? conf.get("encrypt_assets") : data2;
            String[] encryptAssets = new String[0];
            if (data3 != null) {
                JsonArray arr2 = data3.asArray();
                if (arr2.size() > 0) {
                    encryptAssets = new String[arr2.size()];
                    for (int i4 = 0; i4 < encryptAssets.length; i4++) {
                        encryptAssets[i4] = arr2.get(i4).asString();
                    }
                }
            }
            JsonValue data4 = conf.get("removeAssets");
            JsonValue data5 = data4 == null ? conf.get("remove_assets") : data4;
            if (data5 != null) {
                JsonArray arr3 = data5.asArray();
                if (arr3.size() > 0) {
                    int n2 = arr3.size();
                    for (int i5 = 0; i5 < n2; i5++) {
                        CommonUtil.delete(target + "/assets/" + arr3.get(i5).asString());
                    }
                }
            }
            JsonValue data6 = conf.get("remove");
            if (data6 != null) {
                JsonArray arr4 = data6.asArray();
                if (arr4.size() > 0) {
                    int n3 = arr4.size();
                    for (int i6 = 0; i6 < n3; i6++) {
                        CommonUtil.delete(target + "/" + arr4.get(i6).asString());
                    }
                }
            }
            if (sdkVersion.equals("1.0")) {
                CopySdk.copy(sdkPath + "/COMMON", target);
                if (new File(target + "/lib").list().length < 1) {
                    CommonUtil.copy(sdkPath + "/COMMON/lib", target + "/lib", false);
                }
                CommonUtil.encrypt(new File(srcPath + "/default.json"), new File(target + "/assets/raw/dj"));
                ProcessAssets.process(target, encryptAssets);
                ProcessSmaliAndXML.process(target, rootPath2 + "/tools/conf", sdkType, conf);
            }
            writeAndroidManifext(target, postProcessAndroidManifest(readAndroidManifext(target), conf, srcPath, target, pkg));
            return conf;
        }
        throw new Exception("Doesn't find app.json or app.json format is wrong!");
    }

    private static Document postProcessAndroidManifest(Document androidManifestXML, JsonObject conf, String path, String outPath, String pkg) throws Exception {
        JsonValue value;
        Element root = androidManifestXML.getRootElement();
        QName androidName = new QName("name", namespace);
        File removePermissionFile = new File(path + "/remove_permissions.xml");
        if (removePermissionFile.isFile()) {
            List<Element> removePermissions = CommonUtil.readXML(removePermissionFile).getRootElement().elements("uses-permission");
            List<Element> permissions = root.elements("uses-permission");
            for (Element e : permissions) {
                if (removePermissions != null && !removePermissions.isEmpty()) {
                    for (Element t : removePermissions) {
                        if (e.attributeValue(androidName).equals(t.attributeValue(androidName))) {
                            e.detach();
                        }
                    }
                }
            }
        }
        Element myapplication = root.element("application");
        boolean hide_from_recents = conf == null ? false : conf.getInt("hide_from_recents", 0) == 1;
        if (hide_from_recents) {
            List<Element> activities = myapplication.elements("activity");
            QName excludeFromRecents = new QName("excludeFromRecents", CommonUtil.namespace);
            for (Element t2 : activities) {
                t2.setAttributeValue(excludeFromRecents, "true");
            }
        }
        if (conf != null) {
            JsonValue value2 = conf.get("remove_permissions");
            if (value2 != null) {
                JsonArray arr = value2.asArray();
                if (arr.size() > 0) {
                    for (int i = 0; i < arr.size(); i++) {
                        List<Element> permissions2 = root.elements("uses-permission");
                        for (Element e2 : permissions2) {
                            if (e2.attributeValue(androidName).equals(arr.get(i).asString())) {
                                e2.detach();
                            }
                        }
                    }
                }
            }
            String sdk_type = conf.getString("sdk_type", "");
            if (sdk_type.contains("ufacebook") && (value = conf.get("facebook")) != null) {
                JsonObject data = value.asObject();
                String facebook_app_id = data.getString("facebook_app_id", null);
                if (facebook_app_id == null) {
                    throw new Exception("You didn't have set the facebook_app_id in the app.json");
                }
                CommonUtil.modifyMetaData(myapplication, "com.facebook.sdk.ApplicationId", "@string/facebook_app_id");
                File xmlFile = new File(outPath + "/res/values/res.xml");
                if (!xmlFile.isFile()) {
                    xmlFile = new File(outPath + "/res/values/strings.xml");
                }
                if (xmlFile.isFile()) {
                    Document xml = CommonUtil.readXML(xmlFile);
                    CommonUtil.addElement(xml.getRootElement(), "string", facebook_app_id, "name", "facebook_app_id");
                    CommonUtil.writeXML(xmlFile, xml);
                }
            }
            if (sdk_type.contains("mfirebase")) {
                String xmlStr = androidManifestXML.asXML();
                androidManifestXML = DocumentHelper.parseText(xmlStr.replaceAll("yourpackagename", pkg));
                File googlejson = new File(path + "/google-services.json");
                String json = CommonUtil.readFile(googlejson, (String) null);
                if (json == null) {
                    throw new Exception("You haven't put the google-services.json in the target folder.");
                }
                JsonObject gsObj = JsonObject.readFrom(json);
                File xmlFile2 = new File(outPath + "/res/values/res.xml");
                if (!xmlFile2.isFile()) {
                    xmlFile2 = new File(outPath + "/res/values/strings.xml");
                }
                if (xmlFile2.isFile()) {
                    Document xml2 = CommonUtil.readXML(xmlFile2);
                    Element xmlRoot = xml2.getRootElement();
                    JsonArray client = gsObj.get("client").asArray();
                    if (client.size() != 1) {
                        throw new Exception("Plz check your google-services.json, the client size is not equals one.");
                    }
                    JsonObject clientObj = client.get(0).asObject();
                    String google_app_id = clientObj.get("client_info").asObject().getString("mobilesdk_app_id", null);
                    JsonArray arr2 = clientObj.get("oauth_client").asArray();
                    String default_web_client_id = null;
                    int i2 = 0;
                    while (true) {
                        if (i2 >= arr2.size()) {
                            break;
                        }
                        JsonObject obj = arr2.get(i2).asObject();
                        if (obj.getInt("client_type", 0) != 3) {
                            i2++;
                        } else {
                            default_web_client_id = obj.getString("client_id", null);
                            break;
                        }
                    }
                    String google_api_key = null;
                    JsonArray arr3 = clientObj.get("api_key").asArray();
                    for (int i3 = 0; i3 < arr3.size(); i3++) {
                        google_api_key = arr3.get(i3).asObject().getString("current_key", null);
                        if (google_api_key != null) {
                            break;
                        }
                    }
                    String google_crash_reporting_api_key = google_api_key;
                    JsonObject project_info = gsObj.get("project_info").asObject();
                    String firebase_database_url = project_info.getString("firebase_url", null);
                    String gcm_defaultSenderId = project_info.getString("project_number", null);
                    CommonUtil.addElement(xmlRoot, "string", google_app_id, 1, "name", "google_app_id", "translatable", "false");
                    CommonUtil.addElement(xmlRoot, "string", gcm_defaultSenderId, 1, "name", "gcm_defaultSenderId", "translatable", "false");
                    CommonUtil.addElement(xmlRoot, "string", firebase_database_url, 1, "name", "firebase_database_url", "translatable", "false");
                    CommonUtil.addElement(xmlRoot, "string", default_web_client_id, 1, "name", "default_web_client_id", "translatable", "false");
                    CommonUtil.addElement(xmlRoot, "string", google_api_key, 1, "name", "google_api_key", "translatable", "false");
                    CommonUtil.addElement(xmlRoot, "string", google_crash_reporting_api_key, 1, "name", "google_crash_reporting_api_key", "translatable", "false");
                    CommonUtil.writeXML(xmlFile2, xml2);
                }
            }
        }
        return androidManifestXML;
    }

    private static File searchApk(File file) {
        File f = null;
        if (file.isDirectory()) {
            File[] lst = file.listFiles();
            if (lst.length > 0) {
                int j = 0;
                while (true) {
                    if (j >= lst.length) {
                        break;
                    } else if (!lst[j].getName().endsWith(".apk")) {
                        j++;
                    } else {
                        f = lst[j];
                        break;
                    }
                }
            }
        }
        return f;
    }

    public static void build(String root, String type, String appid, String pkgPath) {
        JsonValue value;
        String shaValue;
        File f = new File(CommonUtil.fixPath(root));
        if (!f.isDirectory()) {
            return;
        }
        String pkg = pkgPath;
        if (pkg.indexOf("@") >= 0) {
            pkg = pkg.substring(0, pkg.indexOf("@"));
        }
        rootPath = f.getAbsolutePath();
        System.out.println("===> Start build [" + appid + "]" + pkg + ".apk in " + type);
        File rootFile = new File(rootPath);
        File dir = new File(rootPath + "/" + type);
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        File[] files = dir.listFiles();
        File srcDir = null;
        File originApk = null;
        if (files != null && files.length > 0) {
            for (File _dir : files) {
                if (_dir.isDirectory() && _dir.getName().startsWith(appid + "_")) {
                    srcDir = new File(_dir.getAbsolutePath() + "/pkg/" + pkgPath);
                    originApk = searchApk(new File(srcDir.getAbsolutePath() + "/apk"));
                    if (originApk == null) {
                        originApk = searchApk(new File(_dir.getAbsolutePath() + "/apk"));
                    }
                    update(originApk.getParent());
                }
            }
        }
        try {
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getLocalizedMessage());
            System.out.println("[Error] Can't build the apk, please notice the developer.");
            if (_errlog != null) {
                e.printStackTrace(_errlog);
                _errlog.flush();
            }
        }
        if (srcDir == null) {
            throw new Exception("The target dir " + srcDir + " is not exists!");
        }
        update(srcDir.getAbsolutePath());
        initLogs(srcDir);
        if (originApk == null) {
            throw new Exception("The origin apk is not exists!");
        }
        if (!new File(srcDir.getAbsolutePath() + "/app.json").isFile()) {
            throw new Exception("app.json doesn't exist in " + pkgPath + ", please check!");
        }
        if (originApk != null) {
            String configJson = CommonUtil.readFile(rootFile.getAbsolutePath() + "/tools/conf/config.json", "{}");
            config = JsonObject.readFrom(configJson);
            JsonValue tmp = config.get(type);
            if (tmp == null || !tmp.isObject()) {
                throw new Exception("The config.json doesn't has \"" + type + "\" config!");
            }
            config = tmp.asObject();
            String domains = config.getString("domain", "api.17taptap.com");
            domain = "http://" + domains.split(",")[0];
            String target = srcDir.getAbsolutePath() + "/decompile";
            StringBuffer sb = CommonUtil.doShell(rootFile.getAbsolutePath() + "/tools/shell/unpack.sh " + originApk.getAbsolutePath() + " " + target);
            log(sb.toString());
            StringBuffer sb2 = CommonUtil.doShell(rootFile.getAbsolutePath() + "/tools/shell/keystore.sh " + srcDir.getAbsolutePath() + " " + pkg);
            log(sb2.toString());
            JsonObject conf = prepare(rootFile.getAbsolutePath(), srcDir.getAbsolutePath(), target, pkg, getType(type));
            StringBuffer sb3 = CommonUtil.doShell(rootFile.getAbsolutePath() + "/tools/shell/build.sh " + srcDir.getAbsolutePath() + " " + pkg);
            log(sb3.toString());
            JsonValue tmp2 = conf.get("keystore");
            JsonObject keystore = tmp2 == null ? new JsonObject() : tmp2.asObject();
            String storeName = keystore.getString("name", pkg);
            String alias = keystore.getString("alias", pkg);
            String pass = keystore.getString("pass", "123456");
            String keypass = keystore.getString("keypass", pass);
            if (conf != null && (value = conf.get("facebook")) != null) {
                Document xml = readAndroidManifext(target);
                JsonObject data = value.asObject();
                String keystore_path = srcDir.getAbsolutePath() + "/" + storeName + ".keystore";
                StringBuffer sha = CommonUtil.doShell(String.format("keytool -exportcert -keystore %s -alias %s -storepass %s -keypass %s | openssl sha1 -binary | openssl base64", keystore_path, alias, pass, keypass));
                if (sha != null && (shaValue = sha.toString()) != null) {
                    data.set("sha", shaValue);
                    Element app = xml.getRootElement().element("application");
                    String launcherName = CommonUtil.getLauncherActivityClassPath(app);
                    data.set("launcher", launcherName);
                }
                conf.set("facebook", data);
            }
            CommonUtil.writeFile(new File(srcDir.getAbsoluteFile() + "/app.json"), conf.toString(WriterConfig.PRETTY_PRINT));
            if (conf.getInt("resguard", 0) == 1) {
                String path = srcDir.getAbsoluteFile() + "/resguard.xml";
                if (!CommonUtil.hasFile(path)) {
                    String path2 = rootFile.getAbsolutePath() + "/tools/conf/resguard.xml";
                    CommonUtil.copy(path2, srcDir.getAbsolutePath() + "/resguard.xml", false);
                    CommonUtil.replaceAll(srcDir.getAbsolutePath() + "/resguard.xml", "<your_package_name>", pkg);
                    sb3 = CommonUtil.doShell(rootFile.getAbsolutePath() + "/tools/shell/resguard.sh " + srcDir.getAbsolutePath() + " " + pkg + " " + storeName + " " + alias + " " + pass + " " + keypass);
                }
            } else if (OSInfo.isMacOS()) {
                sb3 = CommonUtil.doShell(rootFile.getAbsolutePath() + "/tools/shell/signapk.mac.sh " + srcDir.getAbsolutePath() + " " + pkg + " " + storeName + " " + alias + " " + pass + " " + keypass);
            } else {
                sb3 = CommonUtil.doShell(rootFile.getAbsolutePath() + "/tools/shell/signapk.sh " + srcDir.getAbsolutePath() + " " + pkg + " " + storeName + " " + alias + " " + pass + " " + keypass);
            }
            log(sb3.toString());
            log(" successs!");
        }
        if (_errlog != null) {
            _errlog.flush();
            _errlog.close();
        }
        if (_infolog != null) {
            _infolog.flush();
            _infolog.close();
        }
        commit(srcDir.getAbsolutePath());
    }

    private static void commit(final String path) {
        new Thread(new Runnable() { // from class: com.ivy.build.ProcessBuild.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Thread.sleep(500L);
                    CommonUtil.delete(path + "/decompile");
                    if (OSInfo.isMacOS()) {
                        System.out.println(CommonUtil.doShell(ProcessBuild.rootPath + "/tools/shell/commit.mac.sh " + path).toString());
                    } else {
                        System.out.println(CommonUtil.doShell(ProcessBuild.rootPath + "/tools/shell/commit.sh " + path).toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void update(String path) {
        if (OSInfo.isMacOS()) {
            System.out.println(CommonUtil.doShell(rootPath + "/tools/shell/update.mac.sh " + path).toString());
        } else {
            System.out.println(CommonUtil.doShell(rootPath + "/tools/shell/update.sh " + path).toString());
        }
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            if (args.length == 1) {
                rootPath = args[0];
                commit(rootPath);
                return;
            }
            return;
        }
        String root = args[0];
        String type = args[1];
        String app = args[2];
        String pkg = args[3];
        build(root, type, app, pkg);
    }
}