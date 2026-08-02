export interface PlatformMeta {
  /** 中文平台名称。 */
  label: string
  /** 平台品牌色。 */
  color: string
  /** 用于界面标识的英文名称。 */
  roman: string
  /** 平台域名。 */
  domain: string
}

/** Figma Make 参考项目中的四个平台展示令牌。 */
export const platformCatalog: Record<'bilibili' | 'douyu' | 'huya' | 'douyin', PlatformMeta> = {
  bilibili: { label: 'B站', color: '#00A1D6', roman: 'BILIBILI', domain: 'live.bilibili.com' },
  douyu: { label: '斗鱼', color: '#FF5500', roman: 'DOUYU', domain: 'www.douyu.com' },
  huya: { label: '虎牙', color: '#FFB800', roman: 'HUYA', domain: 'www.huya.com' },
  douyin: { label: '抖音', color: '#FF2D55', roman: 'DOUYIN', domain: 'live.douyin.com' },
}
