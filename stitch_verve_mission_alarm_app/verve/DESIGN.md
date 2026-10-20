---
name: Wakefully
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#46464b'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#76777b'
  outline-variant: '#c7c6cb'
  surface-tint: '#5e5e62'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#1b1b1f'
  on-primary-container: '#848387'
  inverse-primary: '#c7c6ca'
  secondary: '#a23e18'
  on-secondary: '#ffffff'
  secondary-container: '#fe8357'
  on-secondary-container: '#6f2000'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#181c22'
  on-tertiary-container: '#81848c'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e3e2e6'
  primary-fixed-dim: '#c7c6ca'
  on-primary-fixed: '#1b1b1f'
  on-primary-fixed-variant: '#46464a'
  secondary-fixed: '#ffdbcf'
  secondary-fixed-dim: '#ffb59c'
  on-secondary-fixed: '#390c00'
  on-secondary-fixed-variant: '#822801'
  tertiary-fixed: '#e0e2eb'
  tertiary-fixed-dim: '#c4c6cf'
  on-tertiary-fixed: '#181c22'
  on-tertiary-fixed-variant: '#43474e'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  headline-xl:
    fontFamily: Newsreader
    fontSize: 56px
    fontWeight: '400'
    lineHeight: 60px
    letterSpacing: -0.025em
  headline-xl-mobile:
    fontFamily: Newsreader
    fontSize: 36px
    fontWeight: '400'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Newsreader
    fontSize: 40px
    fontWeight: '400'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Newsreader
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Newsreader
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Hanken Grotesk
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: 0em
  body-sm:
    fontFamily: Hanken Grotesk
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0.005em
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.06em
spacing:
  gutter: 1.5rem
  gutter-desktop: 2.5rem
  margin: 1.25rem
  margin-desktop: 4rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 3rem
---

## Brand & Style

This design system defines an anti-slop, hyper-refined editorial product surface. It replaces the homogenous rounded-card-in-card paradigm with structural clarity, architectural typography, and deliberate white space. The interface behaves like an exacting physical broadsheet translated to a high-density digital display: quiet, decisive, and authoritative.

The aesthetic fuses Japanese minimal reduction with Swiss typographic precision. Surfaces evoke matte, un-calendered chalk paper and warm gallery walls. Information hierarchy relies primarily on stark typographic scale contrast, tabular alignments, and precise hairline rules rather than heavy containers or artificial drop shadows. The emotional cadence is contemplative yet sharp—free of synthetic glows, playful pill tags, or decorative illustration.

## Colors

The color palette is strictly disciplined, prioritizing ink-on-chalk contrast over multi-colored decoration.

- **Primary (`#111215`)**: Deep carbon ink. Used for high-impact display typography, primary callouts, and grounding structural anchor lines.
- **Secondary (`#C85A32`)**: Single-origin warm terracotta/burnt ember. Applied with extreme austerity—restricted exclusively to active states, critical focus markers, and editorial punctuation marks. It is never used across large flood fills.
- **Tertiary (`#6E7179`)**: Muted slate graphite. Used for secondary metadata, tabular captions, structural coordinate labels, and inactive rule lines.
- **Neutral (`#F8F9FA`)**: Warm chalk white. Complemented by `#FFFFFF` for pure illuminated panels and `#F2F3F5` for contextual resting surfaces.

Dividers and borders strictly employ `#111215` at a 10% alpha (`rgba(17, 18, 21, 0.10)`), resolving into razor-sharp hairlines.

## Typography

Typography delivers the primary visual architecture. 

- **Headlines (Newsreader)**: Evokes the quiet intellect of high-end literary broadsheets and critical reviews. Rendered at regular weight (`400`) with precise optical sizing, italicized only for deliberate semantic emphasis or quiet contemplation.
- **Body Text (Hanken Grotesk)**: A neutral, balanced grotesque that dissolves seamlessly during extended reading. Set with generous line heights and neutral kerning.
- **Structural Labels & Metrics (JetBrains Mono)**: Provides clinical micro-typographic tension. Used uppercase for status codes, indexation, data tables, and technical metadata. Numeric tabular figures (`font-variant-numeric: tabular-nums`) must be active by default across all figures and tables.

## Layout & Spacing

Layout conforms to an asymmetrical, disciplined 12-column fixed-max-width grid (capped at 1360px on wide displays). 

- **Desktop (>=1024px)**: 12 columns with `2.5rem` gutters and minimum `4rem` outer margins. Wide content rests on asymmetric columns (e.g., 4 columns for sticky contextual indexation, 8 columns for core editorial reading).
- **Tablet (768px - 1023px)**: 8 columns with `1.5rem` gutters and `2rem` outer margins.
- **Mobile (<768px)**: 4 columns with `1rem` gutters and `1.25rem` outer canvas padding.

Avoid boxing related items inside arbitrary card containers. Establish groupings through rhythmic vertical cadence (`space-xl` between sections, `space-sm` between label and datum) paired with full-width hairline border rules.

## Elevation & Depth

This system operates without simulated dimensional lighting. Blurred ambient drop shadows, colored halos, and frosted glass reflections are completely banned.

Depth is achieved purely through two mechanisms:
1. **Hairline Structural Rules**: `0.5px` to `1px` crisp boundaries rendered via `rgba(17, 18, 21, 0.10)`. No border radius softening at intersections.
2. **Subtle Surface Shifts**: Stacking relies on planar shifts between the canvas base (`#F8F9FA`), pure paper white (`#FFFFFF`), and subdued recessive zones (`#F2F3F5`).

Floating dialogs or dropdown menus rely on clean `#FFFFFF` surfaces wrapped in a single, crisp hairline stroke (`1px solid #111215`) with a direct 1-pixel hard offset or total flat plane alignment.

## Shapes

The design system uses architectural precision: zero roundedness (`roundedness: 0`). 

Every element—including interactive buttons, input fields, state tags, and panels—features exact 90-degree right angles (`border-radius: 0px`). Pill-shaped buttons, circular avatar rings, and softened card shells are strictly prohibited. Visual distinction is dictated by border weight, negative space, and font shifts, never rounded geometries.

## Components

### Buttons
- **Primary**: Sharp rectangular block with `#111215` fill, `#FFFFFF` text, rendered in uppercase `JetBrains Mono` (`label-md`). Padding: `10px 20px`. Transitions are instant (0ms or 100ms ease-out) without bouncy springs.
- **Secondary / Outline**: 1px solid border (`rgba(17, 18, 21, 0.2)`), `#FFFFFF` or transparent background, `#111215` text. On hover: border switches instantly to `#111215`.
- **Tertiary / Ghost**: Zero border, underlined or arrow-notated (`->`) monospaced link in `#C85A32` or `#111215`.

### Chips & Badges
- Replaced by micro-typographic indicators: `label-sm` in `JetBrains Mono`, enclosed in a 1px hairline square box with `2px 6px` inner padding. No pill shapes. Statuses use clean text codes (e.g., `[ACTIVE]`, `[IDLE]`) or a precise `5x5px` square swatch in `#C85A32`.

### Lists & Dividers
- Lists do not use bullet points or floating cards. Items are stacked tabular rows separated by horizontal `0.5px` border rules (`rgba(17, 18, 21, 0.1)`). 
- Row elements align to columns: timestamp/index at Column 1, primary descriptor at Column 3, status/action at the end. Hover states trigger a subtle row background wash to `#F2F3F5`.

### Checkboxes & Radio Buttons
- **Checkboxes**: Pure `14x14px` squares with a 1px border (`#111215`). Selected state fills the square solid with `#111215` and exposes a pure chalk white inner square dot (`4x4px`). No rounded checkmarks.
- **Radio Buttons**: `14x14px` diamond or crisp square boundary with an inverted central square indicator. 

### Input Fields
- Understated baseline or perimeter forms. Single-line bottom border (`1px solid rgba(17, 18, 21, 0.2)`) on a flat canvas, or clean 1px border around the entire box. 
- Focus state sharpens border to `1px solid #111215` with an accent dot in `#C85A32`. Text inputs use `Hanken Grotesk` body copy; helper notes and field keys sit strictly above in uppercase `JetBrains Mono`.

### Cards & Sections
- Eliminate card-in-card nesting. Information groups are defined by negative space and clear section headers composed of a serif headline (`headline-md`) alongside a small monospaced index mark (`01 / SECTION`). When containment is mandatory, apply a single perimeter hairline rule with zero corner radius and a `#FFFFFF` fill over the `#F8F9FA` ground.