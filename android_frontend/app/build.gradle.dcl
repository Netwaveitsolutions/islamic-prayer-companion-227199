androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))

        // UI / AndroidX (Views, Fragments, Material)
        implementation("androidx.core:core-ktx:1.13.1")
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
        implementation("androidx.fragment:fragment-ktx:1.8.2")
        implementation("androidx.activity:activity-ktx:1.9.1")
        implementation("androidx.viewpager2:viewpager2:1.1.0")
        implementation("androidx.preference:preference-ktx:1.2.1")

        // Lifecycle / Coroutines
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

        // Networking (Retrofit/OkHttp) + JSON
        implementation("com.squareup.retrofit2:retrofit:2.11.0")
        implementation("com.squareup.retrofit2:converter-gson:2.11.0")
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
        implementation("com.google.code.gson:gson:2.11.0")

        // Google Sign-In
        implementation("com.google.android.gms:play-services-auth:21.2.0")

        // Media (Tatweej): ExoPlayer for smooth, reusable playback and fast clip switching
        implementation("androidx.media3:media3-exoplayer:1.4.1")
        implementation("androidx.media3:media3-datasource:1.4.1")

        // Unit tests: ensure tests are discoverable by the default test runner (JUnit4)
        implementation("junit:junit:4.13.2")
    }
}
