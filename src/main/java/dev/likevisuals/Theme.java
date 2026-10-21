package dev.likevisuals;

/** Colour palettes for the click GUI. Call update(mode, alpha) once per frame. */
public final class Theme {
    public static final String[] NAMES = {"Glass", "White", "Dark", "Purple", "Sakura", "Ocean", "Mint"};
    public static final int[] SAMPLE = {0xFF6A7488, 0xFFF4F4F8, 0xFF16161C, 0xFF3A2A66, 0xFF4A2840, 0xFF14405E, 0xFF1A5A44};

    public static int panelT, panelB, sideT, sideB, rowHover, rowLine, chip, chipOff;
    public static int text, nameOff, nameOn, label, muted, dim, divider, field;

    private static int fade(int c, float a) { return (((int) (((c >>> 24) & 255) * Math.max(0f, Math.min(1f, a)))) << 24) | (c & 0xFFFFFF); }

    public static void update(int mode, float alpha) {
        nameOn = 0xFFFFFFFF; divider = 0x22FFFFFF; rowLine = 0x18FFFFFF; rowHover = 0x22FFFFFF;
        switch (mode) {
            case 1: // white
                panelT = 0xF2FFFFFF; panelB = 0xE6F2ECF8; sideT = 0x66E4DCF0; sideB = 0x55E4DCF0; chip = 0xFFE2D8EE; chipOff = 0xFFCFC4DC;
                text = 0xFF2E2038; nameOff = 0xFF3A2A46; nameOn = 0xFF1E1228; label = 0xFF3A2A46; muted = 0xFF7C6A8A; dim = 0xFF9A88A8;
                divider = 0x33000000; rowLine = 0x22000000; rowHover = 0x22000000; field = 0x44FFFFFF;
                break;
            case 2: // dark
                panelT = 0xF2161619; panelB = 0xF20C0C10; sideT = 0x88000000; sideB = 0x88000000; chip = 0xFF2E2E3A; chipOff = 0xFF2A2A34;
                text = 0xFFF0F0F5; nameOff = 0xFFDADAE4; label = 0xFFD0D0DC; muted = 0xFF9A9AAA; dim = 0xFF666672; field = 0x66000000;
                break;
            case 3: // purple
                panelT = 0xF0241A44; panelB = 0xF0160F2C; sideT = 0x88100A22; sideB = 0x88100A22; chip = 0xFF3A2C5E; chipOff = 0xFF2E2250;
                text = 0xFFF0EAFF; nameOff = 0xFFDDD4F5; label = 0xFFD2C8EE; muted = 0xFFA79BCB; dim = 0xFF71669A; field = 0x66100A22;
                break;
            case 4: // sakura
                panelT = 0xF0301A3A; panelB = 0xF0200F28; sideT = 0x88180A1E; sideB = 0x88180A1E; chip = 0xFF4A2A52; chipOff = 0xFF3A2042;
                text = 0xFFFFEAF3; nameOff = 0xFFEBD5E6; label = 0xFFE2CCDD; muted = 0xFFB594B0; dim = 0xFF7E6078; field = 0x66180A1E;
                break;
            case 5: // ocean
                panelT = 0xF00E2A44; panelB = 0xF0081A2C; sideT = 0x88061424; sideB = 0x88061424; chip = 0xFF1F4260; chipOff = 0xFF183450;
                text = 0xFFE6F6FF; nameOff = 0xFFCDE6F5; label = 0xFFC3DDEE; muted = 0xFF7FA6BF; dim = 0xFF55778E; field = 0x66041018;
                break;
            case 6: // mint
                panelT = 0xF0123C30; panelB = 0xF00A2A20; sideT = 0x88061A14; sideB = 0x88061A14; chip = 0xFF245445; chipOff = 0xFF1B4136;
                text = 0xFFE9FFF6; nameOff = 0xFFD2F2E4; label = 0xFFC8EADA; muted = 0xFF86B5A2; dim = 0xFF5C8473; field = 0x66041510;
                break;
            default: // glass (frosted, see-through)
                panelT = 0xB0707A90; panelB = 0xA05A6478; sideT = 0x70384050; sideB = 0x70303848; chip = 0xAA3C4458; chipOff = 0x88303848;
                text = 0xFFFFFFFF; nameOff = 0xFFF2F4FA; label = 0xFFE6E9F2; muted = 0xFFC0C6D8; dim = 0xFF98A0B8; field = 0x55202838;
        }
        panelT = fade(panelT, alpha); panelB = fade(panelB, alpha); sideT = fade(sideT, alpha); sideB = fade(sideB, alpha);
    }
}
