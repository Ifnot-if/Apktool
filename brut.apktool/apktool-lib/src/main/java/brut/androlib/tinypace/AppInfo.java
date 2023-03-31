package brut.androlib.tinypace;

public class AppInfo {
    String appName;
    String packageName;
    String versionCode;
    String versionName;
    String sdkVersion;
    String launchActivity;

    public AppInfo() {
    }

    public AppInfo(String appName, String packageName, String versionCode, String versionName, String sdkVersion, String launchActivity) {
        this.appName = appName;
        this.packageName = packageName;
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.sdkVersion = sdkVersion;
        this.launchActivity = launchActivity;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public String getLaunchActivity() {
        return launchActivity;
    }

    public void setLaunchActivity(String launchActivity) {
        this.launchActivity = launchActivity;
    }

    public String toJsonString() {
        return "{" +
            "\"appName\":\"" + appName + "\"," +
            "\"packageName\":\"" + packageName + "\"," +
            "\"versionCode\":\"" + versionCode + "\"," +
            "\"versionName\":\"" + versionName + "\"," +
            "\"sdkVersion\":\"" + sdkVersion + "\"," +
            "\"launchActivity\":\"" + launchActivity + "\"" +
            "}";

    }
}
