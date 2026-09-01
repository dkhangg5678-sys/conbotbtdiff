# Keep no application secrets or verbose network logging in release builds.
-assumenosideeffects class android.util.Log { public static *** *(...); }
