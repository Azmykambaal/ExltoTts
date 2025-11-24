ملخص التغييرات:

- تم تعريف السمة المفقودة `colorBackground` في app/src/main/res/values/attrs.xml لتصحيح فشل ربط الموارد (Android resource linking error) الذي ظهر في تشغيل الإجراءات (ref: b3cff06c65861b4283a03fd01f7fb09835a1d83a).
- إضافة لون افتراضي `your_background_color` في app/src/main/res/values/colors.xml لتسهيل الربط وتهيئة الثيمات عند الحاجة.
- تحديث gradle.properties في جذر المشروع لزيادة حد الذاكرة المخصصة لـ Gradle/R8 إلى -Xmx6g لتقليل احتمال حدوث OutOfMemoryError أثناء دمج ملفات dex (D8/R8).

الهدف:

إصلاح مشكلتين ظهرتا في سجل تشغيل CI: (1) خطأ "attr/colorBackground not found" أثناء ربط الموارد، و(2) OutOfMemoryError من D8 أثناء دمج dex. التعديلات هذه تهدف لتمرير البناء في CI وإتاحة نقطة بداية لإصلاحات ثيمات لاحقة.

تعليمات للمراجع / خطوات تحقق:

1. راجع الملفات المعدلة في الفرع `fix/colorBackground-and-gradle-memory`.
2. شغّل بناء محلي للتأكد: `./gradlew clean assembleRelease --no-daemon --info`.
3. إن استمر ظهور أخطاء مرتبطة بالثيمات، تحقق من ملفات الثيم (مثل app/src/main/res/values/themes.xml أو styles.xml) واستبدل المرجع `?attr/colorBackground` بـ `?android:attr/colorBackground` إن كان المقصود سمة النظام، أو اضف `<item name="colorBackground">@color/your_background_color</item>` ضمن الثيم المناسب.
4. في حال استمرار خطأ الذاكرة على CI، جرّب زيادة Xmx أكثر أو استخدام runners ذات ذاكرة أعلى.

ملاحظات تقنية:

- التغييرات مبدئية ومحافظة: تعريف `attr` يمنع فشل الربط، لكن قد تحتاج لتعريف قيم ثيم محددة لتوافق بصري.
- تم استخدام المرجع إلى سجل التشغيل/الخطأ الأصلي: ref b3cff06c65861b4283a03fd01f7fb09835a1d83a (معلومات الأخطاء ومخرجات الـ CI).

إذا رغبت، أستطيع بعد الدمج:
- إضافة `<item name="colorBackground">` في الثيم الرئيسي أو تعديل الثيم ليتوافق مع مكتبات الطرف الثالث.
- ضبط الـ workflow لإضافة `env: GRADLE_OPTS` إن رغبت في التحكم بالذاكرة من مستوى CI.
