package rip.ysm.imagestream.webp.enc;

final class FindNearMV {
   private FindNearMV() {
   }

   static int above_block_mode(FullGenArrPointer<ModeInfo> mic, int b, int mi_stride) {
      if (b >> 2 == 0) {
         ModeInfo mi = mic.getRel(-mi_stride);

         switch (mi.mbmi.mode) {
            case B_PRED:
               return mi.bmi[b + 12].as_mode();
            case V_PRED:
               return 2;
            case H_PRED:
               return 3;
            case TM_PRED:
               return 1;
            default:
               return 0;
         }
      } else {
         return mic.get().bmi[b - 4].as_mode();
      }
   }

   static int left_block_mode(FullGenArrPointer<ModeInfo> mic, int b) {
      if ((b & 3) == 0) {
         ModeInfo mi = mic.getRel(-1);

         switch (mi.mbmi.mode) {
            case B_PRED:
               return mi.bmi[b + 3].as_mode();
            case V_PRED:
               return 2;
            case H_PRED:
               return 3;
            case TM_PRED:
               return 1;
            default:
               return 0;
         }
      } else {
         return mic.get().bmi[b - 1].as_mode();
      }
   }
}
