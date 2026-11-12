شاشة المسلم - مشروع تطبيق أندرويد (يعمل بدون إنترنت)

الطريقة 1: بدون تثبيت برامج (GitHub)
1) افتح github.com وسجل حساب مجاني.
2) اضغط New repository وسمّه muslim-tv ثم Create.
3) اضغط uploading an existing file وارفع محتوى هذا المجلد كله (بعد فك الضغط)
   مع مجلد .github الموجود فيه.
4) انتظر دقائق وافتح تبويب Actions ثم Build APK.
5) في آخر الصفحة نزّل muslim-tv-apk (يحتوي app-debug.apk).
   إن لم يظهر مجلد .github: اضغط Add file ثم Create new file واكتب المسار
   .github/workflows/build.yml والصق محتوى الملف.

الطريقة 2: Android Studio
افتح هذا المجلد في Android Studio ثم Build > Build APK(s).

التركيب على التلفزيون: انسخ app-debug.apk على فلاشة، وافتحه من مدير الملفات،
وفعّل التثبيت من مصادر غير معروفة. التطبيق يظهر في قائمة التطبيقات باسم "شاشة المسلم".
