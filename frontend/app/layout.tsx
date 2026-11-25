import "./globals.css";

export const metadata = { title: "상담 예약", description: "1:1 상담 예약 서비스" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body><main className="container">{children}</main></body>
    </html>
  );
}
