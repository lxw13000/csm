// 多媒体消息辅助：与 service 的 ContentType（1 文本 / 2 图片 / 3 其他多媒体）配合

/** 由文件 MIME 推断 contentType：图片=2，其余（视频/文件）=3。 */
export function contentTypeOfFile(file: File): number {
  return file.type.startsWith('image/') ? 2 : 3
}

const VIDEO_EXT = ['.mp4', '.webm', '.ogg', '.mov', '.m4v', '.m3u8']

/** 依据地址后缀判断是否视频。 */
export function isVideoUrl(url: string): boolean {
  const u = (url || '').toLowerCase().split('?')[0]
  return VIDEO_EXT.some((e) => u.endsWith(e))
}

/** 从地址中取文件名用于展示。 */
export function fileNameOf(url: string): string {
  const seg = (url || '').split('?')[0].split('/').pop() || '文件'
  try {
    return decodeURIComponent(seg)
  } catch {
    return seg
  }
}
