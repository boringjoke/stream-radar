# 本地字体资源

将项目使用的本地字体文件放在此目录。当前项目使用 TTF 文件，并在 `src/styles/index.css` 中通过 `@font-face` 引用。

当前计划使用的字体：

- Cinzel
- Cinzel Decorative
- EB Garamond
- Noto Serif SC

当前文件：

- `Cinzel-Regular.ttf`
- `CinzelDecorative-Regular.ttf`
- `EBGaramond-Regular.ttf`
- `NotoSerifSC-Regular.ttf`

字体来源和许可证说明请同步放入 `licenses` 子目录。

生产环境通过 Vite 构建后，字体会被复制到 `dist/fonts`，由 Nginx 作为静态资源提供。若后续转换为 `woff2`，需要同步修改 `src/styles/index.css` 中的文件路径和格式声明。
