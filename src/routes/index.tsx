import { createFileRoute } from "@tanstack/react-router";
import {
  Archive,
  Bookmark,
  Download,
  FileCode2,
  FolderLock,
  Github,
  Image as ImageIcon,
  MousePointerClick,
  Search,
  Share2,
  Smartphone,
  Zap,
} from "lucide-react";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "فایل‌یار — فایل منیجر اندروید با Shizuku" },
      {
        name: "description",
        content:
          "فایل‌یار: فایل منیجر حرفه‌ای اندروید با دسترسی به Android/data و Android/obb از طریق Shizuku. راهنمای دریافت و نصب بدون کامپیوتر.",
      },
      { property: "og:title", content: "فایل‌یار — فایل منیجر اندروید با Shizuku" },
      {
        property: "og:description",
        content: "دسترسی کامل به Android/data بدون روت و بدون کامپیوتر. راهنمای نصب قدم‌به‌قدم.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

const getSteps = [
  {
    icon: Github,
    title: "پروژه را به GitHub وصل کنید",
    body: "در Lovable روی دکمه‌ی + کنار کادر پیام بزنید، Project و بعد GitHub را انتخاب کنید و Connect را بزنید. با گوشی هم انجام می‌شود.",
  },
  {
    icon: Zap,
    title: "GitHub خودش اپ را می‌سازد",
    body: "بعد از وصل شدن، ساخت خودکار شروع می‌شود و حدود ۵ دقیقه طول می‌کشد. پیشرفتش را در تب Actions مخزن می‌بینید.",
  },
  {
    icon: Download,
    title: "فایل APK را دانلود کنید",
    body: "در صفحه‌ی مخزن، بخش Releases را باز کنید و از آخرین نسخه، فایل Filyar-1.0.x.apk را دانلود کنید.",
  },
  {
    icon: Smartphone,
    title: "نصب کنید",
    body: "فایل را باز کنید و اگر گوشی پرسید، اجازه‌ی «نصب از منابع ناشناس» را بدهید. نسخه‌های بعدی روی همین نسخه نصب می‌شوند.",
  },
];

const shizukuSteps = [
  "Shizuku را از گوگل‌پلی یا سایت shizuku.rikka.app نصب کنید.",
  "تنظیمات › درباره‌ی تلفن › هفت بار روی «شماره ساخت» بزنید.",
  "به وای‌فای وصل شوید و در «گزینه‌های توسعه‌دهنده»، «اشکال‌زدایی بی‌سیم» را روشن کنید.",
  "در Shizuku «جفت‌سازی» را بزنید و کد «جفت‌سازی با کد» را در اعلان Shizuku وارد کنید.",
  "در Shizuku «شروع» را بزنید، فایل‌یار را باز کنید و اجازه بدهید.",
];

const features = [
  { icon: FolderLock, title: "Android/data و obb", body: "مرور، ویرایش و جابه‌جایی دیتای برنامه‌ها و بازی‌ها" },
  { icon: MousePointerClick, title: "انتخاب چندتایی", body: "کپی، انتقال، حذف و تغییر نام گروهی" },
  { icon: Archive, title: "فشرده‌سازی ZIP", body: "ساخت و استخراج فایل‌های فشرده" },
  { icon: FileCode2, title: "ویرایشگر متن", body: "ویرایش فایل‌های تنظیمات، json و xml" },
  { icon: ImageIcon, title: "نمایش تصاویر", body: "پیش‌نمایش، بزرگ‌نمایی و آیکون برنامه‌ها" },
  { icon: Search, title: "جستجوی عمیق", body: "جستجو در همه‌ی زیرپوشه‌ها" },
  { icon: Bookmark, title: "نشان‌گذاری", body: "دسترسی سریع به پوشه‌های پرکاربرد" },
  { icon: Share2, title: "اشتراک‌گذاری", body: "ارسال و باز کردن فایل‌ها با برنامه‌های دیگر" },
];

function Index() {
  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="bg-hero text-ink-foreground">
        <div className="mx-auto grid max-w-5xl items-center gap-12 px-6 pb-20 pt-14 md:grid-cols-2 md:pt-20">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-ink-foreground/20 px-3 py-1 text-xs text-ink-muted">
              <Zap className="size-3.5 text-accent" />
              با قدرت Shizuku — بدون روت
            </span>
            <h1 className="mt-6 text-5xl font-black leading-tight md:text-6xl">فایل‌یار</h1>
            <p className="mt-4 max-w-md text-lg leading-8 text-ink-muted">
              فایل منیجر حرفه‌ای اندروید که پوشه‌های قفل‌شده‌ی <span dir="ltr">Android/data</span> و{" "}
              <span dir="ltr">Android/obb</span> را هم باز می‌کند.
            </p>
            <a
              href="#get"
              className="mt-8 inline-flex items-center gap-2 rounded-2xl bg-mint-gradient px-6 py-3.5 font-bold text-ink shadow-soft transition-transform hover:-translate-y-0.5"
            >
              <Download className="size-5" />
              دریافت اپ
            </a>
          </div>
          <PhoneMock />
        </div>
      </section>

      <section id="get" className="mx-auto max-w-5xl px-6 py-16">
        <h2 className="text-3xl font-black">از اینجا تا گوشی شما</h2>
        <p className="mt-2 text-muted-foreground">بدون کامپیوتر — همه‌چیز با گوشی انجام می‌شود.</p>
        <ol className="mt-8 grid gap-4 md:grid-cols-2">
          {getSteps.map((s, i) => (
            <li key={s.title} className="relative rounded-3xl border border-border bg-card p-6 shadow-soft">
              <div className="flex items-center gap-3">
                <span className="grid size-11 place-items-center rounded-2xl bg-secondary text-primary">
                  <s.icon className="size-5" />
                </span>
                <span className="text-sm font-bold text-muted-foreground">مرحله‌ی {(i + 1).toLocaleString("fa")}</span>
              </div>
              <h3 className="mt-4 text-lg font-bold">{s.title}</h3>
              <p className="mt-2 leading-7 text-muted-foreground">{s.body}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="bg-ink text-ink-foreground">
        <div className="mx-auto max-w-5xl px-6 py-16">
          <h2 className="text-3xl font-black">روشن کردن Shizuku</h2>
          <p className="mt-2 text-ink-muted">یک بار انجام می‌دهید؛ بعد از هر بار روشن شدن گوشی فقط «شروع» را بزنید.</p>
          <ol className="mt-8 space-y-3">
            {shizukuSteps.map((s, i) => (
              <li key={s} className="flex gap-4 rounded-2xl bg-ink-foreground/5 p-4">
                <span className="grid size-8 shrink-0 place-items-center rounded-xl bg-mint text-sm font-black text-ink">
                  {(i + 1).toLocaleString("fa")}
                </span>
                <span className="leading-8">{s}</span>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="mx-auto max-w-5xl px-6 py-16">
        <h2 className="text-3xl font-black">همه‌ی امکانات</h2>
        <div className="mt-8 grid grid-cols-2 gap-3 md:grid-cols-4">
          {features.map((f) => (
            <div key={f.title} className="rounded-3xl bg-secondary/60 p-5">
              <f.icon className="size-6 text-primary" />
              <h3 className="mt-3 font-bold">{f.title}</h3>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">{f.body}</p>
            </div>
          ))}
        </div>
      </section>

      <footer className="border-t border-border py-8 text-center text-sm text-muted-foreground">
        فایل‌یار — برای اندروید ۸ و بالاتر
      </footer>
    </main>
  );
}

function PhoneMock() {
  const rows = [
    { name: "com.whatsapp", meta: "۱۲ مورد" },
    { name: "com.pubg.imobile", meta: "۴ مورد" },
    { name: "com.instagram.android", meta: "۷ مورد" },
    { name: "org.telegram.messenger", meta: "۹ مورد" },
  ];
  return (
    <div className="mx-auto w-full max-w-[290px] rounded-[2.5rem] border border-ink-foreground/15 bg-ink p-3 shadow-soft">
      <div className="rounded-[2rem] bg-background p-4 text-foreground">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-lg font-black">data</p>
            <p className="text-xs text-muted-foreground">۲۸۴ پوشه • ۰ فایل</p>
          </div>
          <span className="grid size-9 place-items-center rounded-xl bg-hero">
            <Zap className="size-4 text-accent" />
          </span>
        </div>
        <div className="mt-3 flex gap-1 text-xs" dir="rtl">
          <span className="rounded-full px-2 py-1 text-muted-foreground">حافظه</span>
          <span className="rounded-full px-2 py-1 text-muted-foreground">Android</span>
          <span className="rounded-full bg-secondary px-2 py-1 font-bold">data</span>
        </div>
        <ul className="mt-3 space-y-1">
          {rows.map((r) => (
            <li key={r.name} className="flex items-center gap-3 rounded-2xl p-2">
              <span className="grid size-10 place-items-center rounded-xl bg-secondary text-primary">
                <FolderLock className="size-5" />
              </span>
              <span className="min-w-0">
                <span className="block truncate text-sm font-medium" dir="ltr">
                  {r.name}
                </span>
                <span className="block text-xs text-muted-foreground">{r.meta}</span>
              </span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
