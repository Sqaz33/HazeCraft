package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class MaterialConversionTest {
    @Test void opaqueSourceMaterialsDoNotTreatPackedAlbedoAlphaAsTransparency()throws Exception{
        for(String file:new String[]{"haze_v2_color_psd_c9873321.png","hazev2_head_color_psd_67d284c9.png","haze_v2_gun_vmat_g_tcolor_cc1c4812.png"}){
            try(var stream=getClass().getResourceAsStream("/assets/haze/retail/"+file)){
                var image=ImageIO.read(stream);assertNotNull(image);
                for(int y=0;y<image.getHeight();y+=17)for(int x=0;x<image.getWidth();x+=17)assertEquals(255,(image.getRGB(x,y)>>>24)&255,file);
            }
        }
    }
}
