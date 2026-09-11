package rip.ysm.imagestream.avif;

import rip.ysm.imagestream.avif.dec.AStruct;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvifDecoder {
   public BufferedImage read(byte[] data) throws Exception {
      AvifItems items = parseIsoBmff(data);
      if (items == null) {
         return AStruct.getDecodedImage(data);
      }

      byte[] colorAv1 = items.color != null ? items.color : data;
      BufferedImage colorImage = AStruct.getDecodedImage(colorAv1);
      if (items.alpha == null) {
         return colorImage;
      }

      byte[][] alphaPlane = AStruct.getDecodedAlphaPlane(items.alpha);
      return combineColorAndAlpha(colorImage, alphaPlane);
   }

   public BufferedImage read(File imageFile) throws Exception {
      return read(Files.readAllBytes(imageFile.toPath()));
   }

   private static BufferedImage combineColorAndAlpha(BufferedImage color, byte[][] alpha) {
      int w = color.getWidth();
      int h = color.getHeight();
      BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      int ah = alpha.length;
      int aw = ah > 0 ? alpha[0].length : 0;
      for (int y = 0; y < h; y++) {
         int ay = ah == h ? y : Math.min(y * ah / h, ah - 1);
         for (int x = 0; x < w; x++) {
            int rgb = color.getRGB(x, y) & 0x00FFFFFF;
            int ax = aw == w ? x : Math.min(x * aw / w, aw - 1);
            int a = alpha[ay][ax] & 0xFF;
            out.setRGB(x, y, (a << 24) | rgb);
         }
      }
      return out;
   }

   private static AvifItems parseIsoBmff(byte[] data) {
      if (data.length < 16) return null;
      if (data[4] != 'f' || data[5] != 't' || data[6] != 'y' || data[7] != 'p') return null;

      MetaInfo meta = null;
      int pos = 0;
      while (pos + 8 <= data.length) {
         long boxSize = readU32(data, pos);
         int headerSize = 8;
         if (boxSize == 1) {
            if (pos + 16 > data.length) break;
            boxSize = readU64(data, pos + 8);
            headerSize = 16;
         } else if (boxSize == 0) {
            boxSize = data.length - pos;
         }
         if (boxSize < headerSize || pos + boxSize > data.length) break;
         String type = boxType(data, pos + 4);
         if ("meta".equals(type)) {
            meta = parseMeta(data, pos + headerSize + 4, pos + (int) boxSize);
         }
         pos += (int) boxSize;
      }
      if (meta == null) return null;

      AvifItems items = new AvifItems();
      ItemLocation primaryLoc = meta.iloc.get(meta.primaryItemId);
      if (primaryLoc != null && "av01".equals(meta.itemTypes.get(meta.primaryItemId))) {
         items.color = primaryLoc.read(data);
      }

      for (Map.Entry<Integer, Integer> entry : meta.auxlRefs.entrySet()) {
         int fromId = entry.getKey();
         int toId = entry.getValue();
         if (toId != meta.primaryItemId) continue;
         if (!"av01".equals(meta.itemTypes.get(fromId))) continue;
         ItemLocation alphaLoc = meta.iloc.get(fromId);
         if (alphaLoc == null) continue;
         items.alpha = alphaLoc.read(data);
         break;
      }
      return items;
   }

   private static MetaInfo parseMeta(byte[] data, int start, int end) {
      MetaInfo info = new MetaInfo();
      int pos = start;
      while (pos + 8 <= end) {
         long boxSize = readU32(data, pos);
         int headerSize = 8;
         if (boxSize == 1) {
            if (pos + 16 > end) break;
            boxSize = readU64(data, pos + 8);
            headerSize = 16;
         } else if (boxSize == 0) {
            boxSize = end - pos;
         }
         if (boxSize < headerSize || pos + boxSize > end) break;
         String type = boxType(data, pos + 4);
         int bodyStart = pos + headerSize;
         int bodyEnd = pos + (int) boxSize;
         switch (type) {
            case "pitm":
               parsePitm(data, bodyStart, info);
               break;
            case "iloc":
               parseIloc(data, bodyStart, bodyEnd, info);
               break;
            case "iinf":
               parseIinf(data, bodyStart, bodyEnd, info);
               break;
            case "iref":
               parseIref(data, bodyStart, bodyEnd, info);
               break;
            default:
               break;
         }
         pos += (int) boxSize;
      }
      return info;
   }

   private static void parsePitm(byte[] data, int bodyStart, MetaInfo info) {
      int version = data[bodyStart] & 0xFF;
      int p = bodyStart + 4;
      if (version == 0) {
         info.primaryItemId = readU16(data, p);
      } else {
         info.primaryItemId = (int) readU32(data, p);
      }
   }

   private static void parseIloc(byte[] data, int bodyStart, int bodyEnd, MetaInfo info) {
      int version = data[bodyStart] & 0xFF;
      int p = bodyStart + 4;
      int sizes = data[p] & 0xFF;
      int offsetSize = (sizes >> 4) & 0xF;
      int lengthSize = sizes & 0xF;
      int idxAndBase = data[p + 1] & 0xFF;
      int baseOffsetSize = (idxAndBase >> 4) & 0xF;
      int indexSize = (version == 1 || version == 2) ? (idxAndBase & 0xF) : 0;
      p += 2;
      int itemCount;
      if (version < 2) {
         itemCount = readU16(data, p); p += 2;
      } else {
         itemCount = (int) readU32(data, p); p += 4;
      }
      for (int i = 0; i < itemCount && p < bodyEnd; i++) {
         int itemId;
         if (version < 2) {
            itemId = readU16(data, p); p += 2;
         } else {
            itemId = (int) readU32(data, p); p += 4;
         }
         int constructionMethod = 0;
         if (version == 1 || version == 2) {
            int cm = readU16(data, p); p += 2;
            constructionMethod = cm & 0xF;
         }
         p += 2; // data_reference_index
         long baseOffset = readUInt(data, p, baseOffsetSize); p += baseOffsetSize;
         int extentCount = readU16(data, p); p += 2;
         List<long[]> extents = new ArrayList<>();
         for (int e = 0; e < extentCount; e++) {
            if ((version == 1 || version == 2) && indexSize > 0) {
               p += indexSize;
            }
            long extentOffset = readUInt(data, p, offsetSize); p += offsetSize;
            long extentLength = readUInt(data, p, lengthSize); p += lengthSize;
            extents.add(new long[]{ extentOffset, extentLength });
         }
         ItemLocation loc = new ItemLocation();
         loc.constructionMethod = constructionMethod;
         loc.baseOffset = baseOffset;
         loc.extents = extents;
         info.iloc.put(itemId, loc);
      }
   }

   private static void parseIinf(byte[] data, int bodyStart, int bodyEnd, MetaInfo info) {
      int version = data[bodyStart] & 0xFF;
      int p = bodyStart + 4;
      int entryCount;
      if (version == 0) {
         entryCount = readU16(data, p); p += 2;
      } else {
         entryCount = (int) readU32(data, p); p += 4;
      }
      for (int i = 0; i < entryCount && p + 8 <= bodyEnd; i++) {
         long subSize = readU32(data, p);
         if (subSize < 8 || p + subSize > bodyEnd) break;
         String subType = boxType(data, p + 4);
         if ("infe".equals(subType)) {
            parseInfe(data, p + 8, p + (int) subSize, info);
         }
         p += (int) subSize;
      }
   }

   private static void parseInfe(byte[] data, int bodyStart, int bodyEnd, MetaInfo info) {
      int version = data[bodyStart] & 0xFF;
      if (version < 2) return;
      int p = bodyStart + 4;
      int itemId;
      if (version == 2) {
         itemId = readU16(data, p); p += 2;
      } else {
         itemId = (int) readU32(data, p); p += 4;
      }
      p += 2; // item_protection_index
      if (p + 4 > bodyEnd) return;
      String itemType = boxType(data, p);
      info.itemTypes.put(itemId, itemType);
   }

   private static void parseIref(byte[] data, int bodyStart, int bodyEnd, MetaInfo info) {
      int version = data[bodyStart] & 0xFF;
      int p = bodyStart + 4;
      while (p + 8 <= bodyEnd) {
         long refSize = readU32(data, p);
         if (refSize < 8 || p + refSize > bodyEnd) break;
         String refType = boxType(data, p + 4);
         int bp = p + 8;
         int fromId;
         if (version == 0) {
            fromId = readU16(data, bp); bp += 2;
         } else {
            fromId = (int) readU32(data, bp); bp += 4;
         }
         int refCount = readU16(data, bp); bp += 2;
         for (int i = 0; i < refCount; i++) {
            int toId;
            if (version == 0) {
               toId = readU16(data, bp); bp += 2;
            } else {
               toId = (int) readU32(data, bp); bp += 4;
            }
            if ("auxl".equals(refType)) {
               info.auxlRefs.put(fromId, toId);
            }
         }
         p += (int) refSize;
      }
   }

   private static String boxType(byte[] data, int off) {
      return new String(data, off, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
   }

   private static int readU16(byte[] data, int off) {
      return ((data[off] & 0xFF) << 8) | (data[off + 1] & 0xFF);
   }

   private static long readU32(byte[] data, int off) {
      return ((long)(data[off] & 0xFF) << 24)
            | ((long)(data[off + 1] & 0xFF) << 16)
            | ((long)(data[off + 2] & 0xFF) << 8)
            | (long)(data[off + 3] & 0xFF);
   }

   private static long readU64(byte[] data, int off) {
      return ByteBuffer.wrap(data, off, 8).order(ByteOrder.BIG_ENDIAN).getLong();
   }

   private static long readUInt(byte[] data, int off, int size) {
      long v = 0;
      for (int i = 0; i < size; i++) {
         v = (v << 8) | (data[off + i] & 0xFF);
      }
      return v;
   }

   private static class MetaInfo {
      int primaryItemId = -1;
      final Map<Integer, ItemLocation> iloc = new HashMap<>();
      final Map<Integer, String> itemTypes = new HashMap<>();
      final Map<Integer, Integer> auxlRefs = new HashMap<>();
   }

   private static class ItemLocation {
      int constructionMethod;
      long baseOffset;
      List<long[]> extents;

      byte[] read(byte[] file) {
         int total = 0;
         for (long[] e : extents) total += (int) e[1];
         byte[] out = new byte[total];
         int pos = 0;
         for (long[] e : extents) {
            int from = (int) (baseOffset + e[0]);
            int len = (int) e[1];
            System.arraycopy(file, from, out, pos, len);
            pos += len;
         }
         return out;
      }
   }

   private static class AvifItems {
      byte[] color;
      byte[] alpha;
   }
}
