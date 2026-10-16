package dev.local.haze.sim;

/** Body heading for the single forward ground clip and the four directional air clips. */
public final class DashVisuals {
    public static float yaw(String clip,V3 dashDirection,float viewYaw){
        if(dashDirection.horizontal()<1e-9)return viewYaw;
        float direction=(float)Math.toDegrees(Math.atan2(-dashDirection.x(),dashDirection.z()));
        if(clip.endsWith("/dash_ground"))return direction;
        if(clip.endsWith("/dash_air_forward"))return direction;
        if(clip.endsWith("/dash_air_right"))return direction-90;
        if(clip.endsWith("/dash_air_left"))return direction+90;
        if(clip.endsWith("/dash_air_back"))return direction-180;
        return viewYaw;
    }
}
