export type CityCode = 'JB' | 'KUCHING' | 'KL' | 'MELAKA'
export interface CityConfig { code: CityCode; nameEn: string; nameZh: string; latitude: number; longitude: number; zoom: number }
export const cities: CityConfig[] = [
  { code: 'JB', nameEn: 'Johor Bahru', nameZh: '新山', latitude: 1.4927, longitude: 103.7414, zoom: 11 },
  { code: 'KUCHING', nameEn: 'Kuching', nameZh: '古晋', latitude: 1.5533, longitude: 110.3592, zoom: 11 },
  { code: 'KL', nameEn: 'Kuala Lumpur', nameZh: '吉隆坡', latitude: 3.139, longitude: 101.6869, zoom: 11 },
  { code: 'MELAKA', nameEn: 'Melaka', nameZh: '马六甲', latitude: 2.1896, longitude: 102.2501, zoom: 12 }
]
