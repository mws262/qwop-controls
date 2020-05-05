package game.qwop;

public class QWOPConstants {
/*
Best I can tell, the order everything is added to the world is this.
Note that all bodies are done first, and then joints afterwards.
    cluarm
    clfarm (lower arm)
    clcalf
    clfoot
    clthigh
    cbody
    cruarm
    crcalf
    crthigh
    crfarm (lower arm)
    chead
    crfoot
    __id0_ (I think this is the concrete block, but I don't know for sure)
    track1
    track
    track2
    __id1_ (joint right upper to right lower arm)
    lcjoint
    lankle
    rankle
    rcjoint
    rtjoint
    ltjoint
    __id2_ (joint left  upper to left lower arm)
    lajoint (shoulder)
    rajoint
    __id3_ (neck)
*/
    /**
     * Physics engine stepping parameters.
     */
    public static final float timestep = 0.04f;

    /**
     * Number of Box2D solver iterations.
     */
    public static int physIterations = 5;

    /**
     * Units visible to the player are the physics engine units divided by this factor. In other words, everything
     * inside the physics world is WAY bigger than what the player is shown.
     */
    public static final float worldScale = 10f;

    /**
     * Goal distance in physics engine units.
     */
<<<<<<< HEAD
    public static final float goalDistance = 1000f;
=======
    public static final float goalDistance = 2 * 1000f;
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449

    /**
     * AABB bounds.
     */
//    public static final float aabbMinX = -100f, aabbMinY = -30f, aabbMaxX = 5000f, aabbMaxY = 80f;
    // Changed to match Flash game.
    public static final float aabbMinX = -1000f, aabbMinY = -40f, aabbMaxX = 4200f, aabbMaxY = 30f;


    /**
     * Gravity in downward direction.
     */
    public static final float gravityMagnitude = 10f;

    /**
     * Track parameters.
     */
    public static final float trackPosX = -30f, trackPosY = 8.90813f + 3.67f, trackFric = 1f, trackRest = 0.2f,
            trackXDim = 1050, trackYDim = 3.67f;

    /**
     * Foot parameters.
     */
    public static final float
            rFootPosX = -0.9675f, // -0.96750f,
            rFootPosY = 7.772499999999999f, // 7.77200f,
            lFootPosX = 3.7625f, // 3.763f,
            lFootPosY = 8.10125f, // 8.101f,
            rFootAng = 0.7498215270272978f, // 0.7498f,
            lFootAng = 0.1428945714492916f; // 0.1429f;
    public static final float
            rFootMass = 11.63015625f, //11.630f,
            rFootInertia = 9.01672687841797f, //9.017f,
            rFootL = 2.6875f, // 2.68750f,
            rFootH = 1.4425f, // 1.44249f,
            rFootFric = 1.5f,
            rFootDensity = 3f,
            lFootMass = 10.894537499999998f, //10.895f,
            lFootInertia = 8.242426898535154f, //8.242f,
            lFootL = 2.695f, // 2.695f,
            lFootH = 1.3475f, // 1.34750f,
            lFootFric = 1.5f,
            lFootDensity = 3f;

    /**
     * Shank parameters.
     */
    public static final float
            rCalfPosX = 0.08499999999999999f, // 0.0850f,
            rCalfPosY = 5.38125f, // 5.381f,
            lCalfPosX = 2.98625f, // 2.986f,
            lCalfPosY = 5.5225f; // 5.523f;
    public static final float
            rCalfAng = -0.820983055566978f, // -0.821f,
            lCalfAng = -1.581538187051731f, // -1.582f,
            rCalfAngAdj = 1.606188724f,
            lCalfAngAdj = 1.607108307f,
            rCalfMass = 7.40653125f, // 7.407f,
            lCalfMass = 7.463787499999999f, // 7.464f,
            rCalfInertia = 16.644328315954585f, //16.644f,
            lCalfInertia = 16.89308957772786f; //16.893f;
    // Length and width for the calves are just for collisions with the ground, so not very important.
    public static final float
            rCalfL = 4.975f, // 4.21f, NOTE Y
            lCalfL = 4.9925f, // 4.43f, NOTE WAS X
            rCalfW = 1.48875f, // 0.4f, NOTE Y
            lCalfW = 1.495f, //0.4f, // NOTE THIS WAS Y
            rCalfFric = 0.2f, lCalfFric = 0.2f, rCalfDensity = 1f, lCalfDensity = 1f;

    /**
     * Thigh parameters.
     */
    public static final float
            rThighPosX = 1.65875f, // 1.659f,
            rThighPosY = 1.99875f, // 1.999f,
            lThighPosX = 2.52f, // 2.52f,
            lThighPosY = 1.6149999999999998f, // 1.615f,
            rThighAng = 1.4683575691488897f, // 1.468f,
            lThighAng = -1.976849563463714f, // -1.977f,
            rThighAngAdj = -1.544382589f,
            lThighAngAdj = 1.619256373f,
            rThighMass = 10.540325000000001f, // 10.54f,
            lThighMass = 10.037218750000001f, // 10.037f,
            rThighInertia = 28.06726599798177f, // 28.067f,
            lThighInertia = 24.54640530426432f; // 24.546f;
    // Length and width for the calves are just for collisions with the ground, so not very important.
    public static final float
            rThighL = 5.29f, // 4.19f, NOTE X
            lThighL = 5.0375f, // 3.56f, NOTE X
            rThighW = 1.9925f,  // 0.6f, NOTE Y
            lThighW = 1.9925f, // 0.6f, NOTE Y
            rThighFric = 0.2f, lThighFric = 0.2f, rThighDensity = 1f, lThighDensity = 1f;

    /**
     * Torso parameters.
     */
    public static final float
            torsoPosX = 2.525f, // 2.525f,
            torsoPosY = -1.92625f, // -1.926f,
            torsoAng = -1.2509879641316413f, // -1.251f,
            torsoAngAdj = 1.651902129f,
            torsoMass = 18.6675f, // 18.668f,
            torsoInertia = 79.37576562499999f; // 79.376f;
    public static final float
            torsoL = 6.55f, // 5f, y
            torsoW = 2.85f, //  1.5f, x
            torsoFric = 0.2f,
            torsoDensity = 1f;

    /**
     * Head parameters.
     */
    public static final float
            headPosX = 3.8962499999999998f, // 3.896f,
            headPosY = -5.67875f, // -5.679f,
            headAng = 0.05799459379421954f, //0.058f,
            headAngAdj = 0.201921414f,
            headMass = 5.673525f, // 5.674f,
            headInertia = 5.4830127584375f; // 5.483f;
    // Radius is just for collision shape
    public static final float headR = 1.1f;
    public static final float // Only realized (or cared) as of 4/3/19 that the head shape is also poly, not circle.
            // This should not affect the dynamics unless the head hits the ground, but knowing QWOP, it might
            // somehow affect the solver dynamics.
            headL = 2.645f, // y
            headW = 2.145f; // x
    public static final float headFric = 0.2f, headDensity = 1f;

    /**
     * Upper arm parameters.
     */
    public static final float
            rUArmPosX = 1.165f, // 1.165f,
            rUArmPosY = -3.61625f, // -3.616f,
            lUArmPosX = 4.475f, // 4.475f,
            lUArmPosY = -2.91125f, // -2.911f,
            rUArmAng = -0.4656111060229135f, // -0.466f,
            lUArmAng = 0.8433813055989805f, // 0.843f,
            rUArmAngAdj = 1.571196588f,
            lUArmAngAdj = -1.690706418f,
            rUArmMass = 5.83650625f, // 5.837f,
            lUArmMass = 4.6065f, // 4.6065f,
            rUArmInertia = 8.478990534381511f, // 8.479f,
            lUArmInertia = 5.850264596875f; // 5.85f;
    // Dimensions for collision shapes
    public static final float
            rUArmL = 3.8975f, // 2.58f, x
            lUArmL = 3.7f, // 2.68f, x
            rUArmW = 1.4975f, // 0.2f, y
            lUArmW = 1.245f, // 0.15f, y
            rUArmFric = 0.2f, lUArmFric = 0.2f, rUArmDensity = 1f, lUArmDensity = 1f;

    /**
     * Lower arm parameters.
     */
    public static final float
            rLArmPosX = 0.36625f, // 0.3662f,
            rLArmPosY = -1.2475f, // -1.248f,
            lLArmPosX = 5.89875f, // 5.899f,
            lLArmPosY = -3.06f, // -3.06f,
            rLArmAng = -1.7624310072503824f, // -1.762f,
            lLArmAng = -1.2509879641316413f, // -1.251f,
            rLArmAngAdj = 1.521319096f,
            lLArmAngAdj = 1.447045854f,
            rLArmMass = 5.9896375f, // 5.99f,
            lLArmMass = 3.8445000000000005f, // 3.8445f,
            rLArmInertia = 10.768260765983074f, // 10.768f,
            lLArmInertia = 4.301042384375001f; // 4.301f;
    // For collision shapes
    public static final float
            rLArmL = 4.445f,  // 3.56f, x
            lLArmL = 3.495f, // 2.54f,
            rLArmW = 1.3475f, // 0.15f, y
            lLArmW = 1.1f, // 0.12f, y
            rLArmFric = 0.2f, lLArmFric = 0.2f, rLArmDensity = 1f, lLArmDensity = 1f;

    /**
     * Joint speed setpoints.
     */
    public static final float rAnkleSpeed1 = 2f, rAnkleSpeed2 = -2f, lAnkleSpeed1 = -2f, lAnkleSpeed2 = 2f,
            rKneeSpeed1 = -2.5f, rKneeSpeed2 = 2.5f, lKneeSpeed1 = 2.5f, lKneeSpeed2 = -2.5f,
            rHipSpeed1 = -2.5f, rHipSpeed2 = 2.5f, lHipSpeed1 = 2.5f, lHipSpeed2 = -2.5f,
            rShoulderSpeed1 = 2f, rShoulderSpeed2 = -2f, lShoulderSpeed1 = -2f, lShoulderSpeed2 = 2f;

    /**
     * Joint limits.
     */
    public static final float oRHipLimLo = -1.3f, oRHipLimHi = 0.7f, oLHipLimLo = -1f, oLHipLimHi = 1f, //O Hip
    // limits (changed to this when o is pressed):
    pRHipLimLo = -0.8f, pRHipLimHi = 1.2f, pLHipLimLo = -1.5f, pLHipLimHi = 0.5f; //P Hip limits

    /**
     * Springs and things.
     */
    public static final float neckStiff = 15f, neckDamp = 5f, rElbowStiff = 1f, lElbowStiff = 1f, rElbowDamp = 0f,
            lElbowDamp = 0f;

    /**
     * Initial conditions.
     */
    public static final float
            rAnklePosX = -1.395f, // -0.96750f,
            rAnklePosY = 7.090000000000001f, // 7.77200f,
            lAnklePosX = 3.15125f, // 3.763f,
            lAnklePosY = 7.946250000000001f, // 8.101f,

            rKneePosX = 1.5800000000000005f, // 1.58f,
            rKneePosY = 4.1137500000000005f, // 4.11375f,
            lKneePosX = 3.2625f, // 3.26250f,
            lKneePosY = 3.5162500000000003f, // 3.51625f,

            rHipPosX = 1.26f, // 1.260f,
            rHipPosY = -0.06750000000000012f, // -0.06750f,
            lHipPosX = 2.0162500000000003f, // 2.01625f,
            lHipPosY = 0.1825000000000001f, // 0.18125f,

            rShoulderPosX = 2.24375f, // 2.24375f,
            rShoulderPosY = -4.1425f, // -4.14250f,
            lShoulderPosX = 3.6387500000000004f, // 3.63875f,
            lShoulderPosY = -3.58875f, // -3.58875f,

            rElbowPosX = -0.06f, // -0.06f,
            rElbowPosY = -2.9850000000000003f, // -2.985f,
            lElbowPosX = 5.65125f, // 5.65125f,
            lElbowPosY = -1.8125f, // -1.8125f,

            neckPosX = 3.6037500000000002f, // 3.60400f,
            neckPosY = -4.58125f; // -4.581f;

    /**
     * Angle failure limits. Fail if torso angle is too big or small to rule out stupid hopping that eventually falls.
     */
    public static final float torsoAngUpper = 1.57f, torsoAngLower = -2.2f; // Negative is falling backwards. 0.4 is start angle.

}
