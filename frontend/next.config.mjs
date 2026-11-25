/** 정적 export: nginx가 / 와 /admin 을 파일로 서빙하고 /api 는 Spring Boot로 프록시 */
const nextConfig = {
  output: "export",
  trailingSlash: true,
};
export default nextConfig;
