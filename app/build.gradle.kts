plugins { id("com.android.application") }
android {
    namespace = "com.whw.lunarbirthday"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.whw.lunarbirthday"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "com.whw.lunarbirthday.DateChecks"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies { implementation("cn.6tail:lunar:1.7.7") }
