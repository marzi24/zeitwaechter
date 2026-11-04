# Erzeugt app/src/main/res/layout/activity_main.xml für Zeitwächter 2.0
import textwrap

NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'


def ind(s, n):
    return textwrap.indent(s, ' ' * n)


def attrs(**kw):
    out = []
    for k, v in kw.items():
        if v is None:
            continue
        k = k.replace('__', ':').replace('_', '_')
        out.append(f'{k}="{v}"')
    return out


def el(tag, a, children=None):
    head = f'<{tag}\n' + '\n'.join('    ' + x for x in a)
    if not children:
        return head + ' />\n'
    return head + '>\n\n' + ind(''.join(children), 4) + f'</{tag}>\n\n'


def tv(id=None, text=None, style=None, w='wrap_content', h='wrap_content', extra=()):
    a = []
    if id:
        a.append(f'android:id="@+id/{id}"')
    if style:
        a.append(f'style="@style/{style}"')
    a += [f'android:layout_width="{w}"', f'android:layout_height="{h}"']
    if text is not None:
        a.append(f'android:text="{text}"')
    a += list(extra)
    return el('TextView', a)


def ll(children, vertical=True, w='match_parent', h='wrap_content', id=None, style=None, extra=()):
    a = []
    if id:
        a.append(f'android:id="@+id/{id}"')
    if style:
        a.append(f'style="@style/{style}"')
    else:
        a += [f'android:layout_width="{w}"', f'android:layout_height="{h}"']
        a.append(f'android:orientation="{"vertical" if vertical else "horizontal"}"')
    a += list(extra)
    return el('LinearLayout', a, children)


def section(children, id=None, extra=()):
    return ll(children, style='Section', id=id, extra=extra)


def icon(name, id=None, size='22dp', tint='@color/text_secondary', extra=()):
    a = []
    if id:
        a.append(f'android:id="@+id/{id}"')
    a += [f'android:layout_width="{size}"', f'android:layout_height="{size}"',
          f'android:src="@drawable/ic_{name}"', f'android:tint="{tint}"',
          'android:importantForAccessibility="no"']
    a += list(extra)
    return el('ImageView', a)


def btn(id, text, style='BtnTonal', w='match_parent', extra=()):
    a = [f'android:id="@+id/{id}"', f'style="@style/{style}"', f'android:layout_width="{w}"',
         f'android:text="{text}"'] + list(extra)
    return el('Button', a)


def label(text):
    return tv(text=text, style='Label')


def hint(text, id=None):
    return tv(id=id, text=text, style='Hint')


def field(id, itype, hint_=None):
    a = [f'android:id="@+id/{id}"', 'style="@style/Field"', f'android:inputType="{itype}"']
    if hint_:
        a.append(f'android:hint="{hint_}"')
    return el('EditText', a)


def lfield(lbl, id, itype, hint_=None):
    return label(lbl) + field(id, itype, hint_)


def space(h):
    return el('View', [ 'android:layout_width="match_parent"', f'android:layout_height="{h}"'])


def divider():
    return el('View', ['android:layout_width="match_parent"', 'android:layout_height="1dp"',
                       'android:layout_marginStart="68dp"', 'android:background="@color/line"'])


# =====================================================================
# HEUTE
# =====================================================================
heute_header = ll([
    tv(text='Heute', style='ScreenTitle'),
    tv(id='tvDate', extra=['android:textColor="@color/text_secondary"', 'android:textSize="14sp"',
                           'android:layout_marginTop="2dp"']),
], extra=['android:paddingStart="4dp"', 'android:paddingTop="10dp"', 'android:paddingBottom="14dp"'])

setup_hint = ll([
    icon('alert', size='22dp', tint='@color/warn'),
    tv(text='Einrichtung noch nicht vollständig – hier tippen, um sie abzuschließen.', w='0dp',
       extra=['android:layout_weight="1"', 'android:layout_marginStart="12dp"', 'android:textColor="@color/text"',
              'android:textSize="14sp"']),
    icon('chevron', size='20dp'),
], vertical=False, id='tvSetupHint', extra=[
    'android:background="@drawable/warn_bg"', 'android:gravity="center_vertical"', 'android:padding="14dp"',
    'android:layout_marginBottom="12dp"', 'android:visibility="gone"', 'android:clickable="true"',
    'android:focusable="true"'])

alert = ll([
    icon('alert', size='26dp', tint='@color/danger'),
    ll([
        tv(id='tvAlertTitle', text='Gleich ausstempeln!', extra=['android:textColor="@color/text"',
                                                                   'android:textSize="15sp"', 'android:textStyle="bold"']),
        tv(id='tvAlertText', extra=['android:textColor="@color/danger"', 'android:textSize="13.5sp"',
                                    'android:textStyle="bold"', 'android:layout_marginTop="2dp"']),
    ], w='0dp', extra=['android:layout_weight="1"', 'android:layout_marginStart="12dp"']),
], vertical=False, id='alertCard', extra=[
    'android:background="@drawable/bg_alert"', 'android:gravity="center_vertical"', 'android:padding="14dp"',
    'android:layout_marginBottom="12dp"', 'android:visibility="gone"'])

ring_center = ll([
    tv(id='tvRingSmall', text='noch', extra=['android:textColor="@color/text_secondary"', 'android:textSize="13sp"',
                                            'android:textStyle="bold"']),
    tv(id='tvRingBig', text='—', extra=['android:textColor="@color/text"', 'android:textSize="52sp"',
                                       'android:textStyle="bold"', 'android:letterSpacing="-0.03"',
                                       'android:fontFeatureSettings="tnum"', 'android:includeFontPadding="false"']),
    tv(id='tvRingUnit', text='Stunden', extra=['android:textColor="@color/text_secondary"', 'android:textSize="13sp"']),
], w='match_parent', h='match_parent', extra=['android:gravity="center"'])

hero = section([
    tv(id='tvStatusChip', style='Chip', text='Nicht eingestempelt', extra=['android:background="@drawable/bg_chip_primary"',
                                                                        'android:textColor="@color/primary"']),
    el('FrameLayout', ['android:layout_width="228dp"', 'android:layout_height="228dp"', 'android:layout_marginTop="10dp"'], [
        el('de.marcel.zeitwaechter.RingView', ['android:id="@+id/ringView"', 'android:layout_width="match_parent"',
                                               'android:layout_height="match_parent"']),
        ring_center,
    ]),
    tv(id='tvPlusChip', style='Chip', extra=['android:background="@drawable/bg_chip_danger"', 'android:textColor="@color/danger"',
                                             'android:layout_marginTop="4dp"', 'android:layout_marginBottom="12dp"']),
    ll([
        tv(id='tvDeadlineLabel', text='Spätestens raus', extra=['android:textColor="@color/text_secondary"',
                                                                 'android:textSize="15sp"']),
        tv(id='tvDeadline', extra=['android:textColor="@color/text"', 'android:textSize="22sp"', 'android:textStyle="bold"',
                                   'android:layout_marginStart="8dp"', 'android:fontFeatureSettings="tnum"']),
    ], vertical=False, w='wrap_content', extra=['android:gravity="center_vertical"']),
    tv(id='tvDeadlineNote', extra=['android:textColor="@color/text_secondary"', 'android:textSize="12.5sp"',
                                   'android:layout_marginTop="4dp"', 'android:gravity="center"']),
], extra=['android:gravity="center_horizontal"', 'android:paddingTop="20dp"'])


def tile(lbl, id, last=False):
    return ll([tv(text=lbl, style='TileLabel'), tv(id=id, text='—', style='TileValue')], w='0dp', extra=[
        'android:layout_weight="1"', 'android:background="@drawable/bg_tile"', 'android:padding="12dp"'] +
        ([] if last else ['android:layout_marginEnd="8dp"']))


tiles = ll([tile('Anwesend', 'tvStatGross'), tile('Netto', 'tvStatNet'), tile('Heute ±', 'tvStatDiff', True)],
           vertical=False, extra=['android:layout_marginBottom="12dp"'])

stepper_btn = lambda id, text: el('TextView', [f'android:id="@+id/{id}"', 'android:layout_width="40dp"',
                                               'android:layout_height="36dp"', 'android:gravity="center"',
                                               f'android:text="{text}"', 'android:textColor="@color/text"',
                                               'android:textSize="20sp"', 'android:textStyle="bold"',
                                               'android:background="@drawable/bg_pill_btn"', 'android:clickable="true"',
                                               'android:focusable="true"'])
active_box = ll([
    ll([
        ll([
            tv(id='tvStartLabel', text='Eingestempelt', extra=['android:textColor="@color/text_secondary"', 'android:textSize="13sp"']),
            tv(id='tvStartTime', extra=['android:textColor="@color/text"', 'android:textSize="21sp"',
                                        'android:textStyle="bold"', 'android:fontFeatureSettings="tnum"']),
            tv(id='tvPlaceChip', style='Chip', extra=['android:background="@drawable/bg_chip_primary"',
                                                      'android:textColor="@color/primary"', 'android:layout_marginTop="6dp"',
                                                      'android:clickable="true"', 'android:focusable="true"']),
        ], w='0dp', extra=['android:layout_weight="1"']),
        ll([
            stepper_btn('btnMinus', '−'),
            el('TextView', ['android:id="@+id/btnEditStart"', 'android:layout_width="wrap_content"',
                            'android:layout_height="36dp"', 'android:gravity="center"', 'android:paddingStart="14dp"',
                            'android:paddingEnd="14dp"', 'android:text="Ändern"', 'android:textColor="@color/text"',
                            'android:textSize="14sp"', 'android:textStyle="bold"', 'android:background="@drawable/bg_pill_card"',
                            'android:clickable="true"', 'android:focusable="true"']),
            stepper_btn('btnPlus', '+'),
        ], vertical=False, w='wrap_content', extra=['android:background="@drawable/bg_pill"', 'android:padding="4dp"',
                                                    'android:gravity="center_vertical"']),
    ], vertical=False, extra=['android:gravity="center_vertical"']),
    ll([
        btn('btnEnd', 'Ausgestempelt', 'BtnFilled', w='0dp', extra=['android:layout_weight="1"']),
        btn('btnDiscard', 'Verwerfen', 'BtnTonal', w='0dp', extra=['android:layout_weight="0.62"',
                                                                   'android:layout_marginStart="8dp"']),
    ], vertical=False, extra=['android:layout_marginTop="14dp"']),
], id='activeBox')

start_card = section([
    active_box,
    btn('btnStart', 'Jetzt einstempeln', 'BtnFilled', extra=['android:visibility="gone"']),
])

week_card = section([
    ll([
        tv(text='Diese Woche', style='SectionTitle', w='0dp', extra=['android:layout_weight="1"',
                                                                     'android:layout_marginBottom="0dp"']),
        tv(id='tvWeekChip', style='Chip', extra=['android:background="@drawable/bg_chip_ok"', 'android:textColor="@color/ok"']),
    ], vertical=False, extra=['android:gravity="center_vertical"']),
    el('de.marcel.zeitwaechter.WeekBarsView', ['android:id="@+id/weekBars"', 'android:layout_width="match_parent"',
                                               'android:layout_height="wrap_content"', 'android:layout_marginTop="10dp"']),
])

foot_heute = tv(text='Die App ist nur eine Erinnerungshilfe. Maßgeblich ist immer die Zeit an der Stempeluhr. '
                     'Alle Daten bleiben auf diesem Handy.', style='Hint',
                extra=['android:layout_marginBottom="20dp"', 'android:textSize="12sp"', 'android:paddingStart="4dp"'])


def scroll(id, children, gone=False):
    a = [f'android:id="@+id/{id}"', 'android:layout_width="match_parent"', 'android:layout_height="match_parent"',
         'android:clipToPadding="false"']
    if gone:
        a.append('android:visibility="gone"')
    inner = ll(children, extra=['android:paddingStart="16dp"', 'android:paddingEnd="16dp"'])
    return el('ScrollView', a, [inner])


scroll_heute = scroll('scrollHeute', [heute_header, setup_hint, alert, hero, tiles, start_card, week_card, foot_heute])

# =====================================================================
# ARBEITSZEIT
# =====================================================================
round_btn = lambda id, ic, desc: el('ImageButton', [f'android:id="@+id/{id}"', 'android:layout_width="44dp"',
                                                    'android:layout_height="44dp"', 'android:background="@drawable/bg_round"',
                                                    f'android:src="@drawable/ic_{ic}"', 'android:tint="@color/text"',
                                                    f'android:contentDescription="{desc}"'])
zeit_header = ll([
    tv(text='Arbeitszeit', style='ScreenTitle', w='0dp', extra=['android:layout_weight="1"']),
    round_btn('btnExport', 'share', 'Als CSV teilen'),
], vertical=False, extra=['android:gravity="center_vertical"', 'android:paddingStart="4dp"', 'android:paddingTop="10dp"',
                          'android:paddingBottom="12dp"'])

arrow = lambda id, ic, desc: el('ImageButton', [f'android:id="@+id/{id}"', 'android:layout_width="38dp"',
                                                'android:layout_height="38dp"', 'android:background="@drawable/bg_btn_tonal"',
                                                f'android:src="@drawable/ic_{ic}"', 'android:tint="@color/text_secondary"',
                                                f'android:contentDescription="{desc}"'])
month_sel = ll([
    arrow('btnPrevMonth', 'back', 'Vorheriger Monat'),
    tv(id='tvMonth', w='0dp', extra=['android:layout_weight="1"', 'android:gravity="center"', 'android:textColor="@color/text"',
                                     'android:textSize="16sp"', 'android:textStyle="bold"']),
    arrow('btnNextMonth', 'chevron', 'Nächster Monat'),
], vertical=False, extra=['android:background="@drawable/bg_btn_tonal"', 'android:backgroundTint="@color/card"',
                          'android:padding="6dp"', 'android:gravity="center_vertical"', 'android:layout_marginBottom="12dp"'])


def sum_col(lbl, id):
    return ll([tv(text=lbl, style='TileLabel'), tv(id=id, text='—', style='TileValue', extra=['android:textSize="18sp"'])],
              w='0dp', extra=['android:layout_weight="1"'])


summary = section([
    tv(text='Überstunden im Monat', style='SectionTitle', extra=['android:layout_marginBottom="2dp"']),
    tv(id='tvMonthOt', text='—', extra=['android:textColor="@color/ok"', 'android:textSize="36sp"', 'android:textStyle="bold"',
                                        'android:letterSpacing="-0.02"', 'android:fontFeatureSettings="tnum"']),
    el('de.marcel.zeitwaechter.SegmentBarView', ['android:id="@+id/segBar"', 'android:layout_width="match_parent"',
                                                 'android:layout_height="wrap_content"', 'android:layout_marginTop="12dp"']),
    tv(id='tvLegend', w='match_parent', extra=['android:layout_marginTop="10dp"', 'android:lineSpacingExtra="4dp"',
                                               'android:textColor="@color/text_secondary"', 'android:textSize="12.5sp"']),
    ll([sum_col('Ist', 'tvIst'), sum_col('Soll', 'tvSoll'), sum_col('Konto Ende', 'tvKontoEnde')], vertical=False,
       extra=['android:layout_marginTop="14dp"']),
    el('View', ['android:layout_width="match_parent"', 'android:layout_height="1dp"', 'android:background="@color/line"',
                'android:layout_marginTop="14dp"']),
    tv(id='tvPlaces', w='match_parent', extra=['android:layout_marginTop="12dp"', 'android:lineSpacingExtra="4dp"',
                                               'android:textColor="@color/text"', 'android:textSize="13.5sp"']),
])

day_list = ll([], id='dayList', extra=['android:background="@drawable/section_bg"', 'android:paddingTop="6dp"',
                                       'android:paddingBottom="6dp"', 'android:layout_marginBottom="12dp"'])

konto = section([
    tv(id='tvAccTitle', text='Überstundenkonto', style='SectionTitle'),
    ll([], id='accRows'),
])

payout_card = ll([
    tv(id='tvPayoutTitle', style='SectionTitle', extra=['android:textColor="@color/on_payout"', 'android:alpha="0.85"',
                                                        'android:layout_marginBottom="2dp"']),
    tv(id='tvPayoutAmount', extra=['android:textColor="@color/on_payout"', 'android:textSize="42sp"', 'android:textStyle="bold"',
                                   'android:letterSpacing="-0.03"', 'android:fontFeatureSettings="tnum"']),
    tv(id='tvPayoutDetail', w='match_parent', extra=['android:textColor="@color/on_payout"', 'android:alpha="0.92"',
                                                     'android:textSize="13.5sp"', 'android:lineSpacingExtra="3dp"',
                                                     'android:layout_marginTop="2dp"']),
    ll([
        tv(id='tvPayoutPill1', style='Chip', extra=['android:background="@drawable/bg_chip_payout"',
                                                    'android:textColor="@color/on_payout"', 'android:layout_marginEnd="6dp"']),
        tv(id='tvPayoutPill2', style='Chip', extra=['android:background="@drawable/bg_chip_payout"',
                                                    'android:textColor="@color/on_payout"']),
    ], vertical=False, id='payoutPills', extra=['android:layout_marginTop="12dp"']),
], extra=['android:background="@drawable/payout_card"', 'android:padding="20dp"', 'android:layout_marginBottom="12dp"'])

foot_zeit = tv(text='Sollzeit gilt an Tagen Mo–Fr mit Eintrag. Tage ohne Eintrag (frei, Urlaub, krank) zählen nicht. '
                    'Beträge sind Schätzungen brutto vor Steuern und Abgaben.', style='Hint',
               extra=['android:textSize="12sp"', 'android:paddingStart="4dp"', 'android:layout_marginBottom="96dp"'])

scroll_zeit = scroll('scrollZeit', [zeit_header, month_sel, summary, day_list, konto, payout_card, foot_zeit], gone=True)

fab = el('TextView', ['android:id="@+id/btnFab"', 'android:layout_width="wrap_content"', 'android:layout_height="56dp"',
                      'android:layout_gravity="bottom|end"', 'android:layout_margin="18dp"',
                      'android:background="@drawable/bg_fab"', 'android:elevation="6dp"',
                      'android:gravity="center_vertical"', 'android:paddingStart="18dp"', 'android:paddingEnd="22dp"',
                      'android:drawableStart="@drawable/ic_plus"', 'android:drawableTint="@color/on_primary"',
                      'android:drawablePadding="8dp"', 'android:text="Eintrag"', 'android:textColor="@color/on_primary"',
                      'android:textSize="15sp"', 'android:textStyle="bold"', 'android:clickable="true"',
                      'android:focusable="true"', 'android:visibility="gone"'])

# =====================================================================
# EINSTELLUNGEN – Liste
# =====================================================================


def srow(id, ic, title, sub_id, right='chevron'):
    r = []
    r.append(el('FrameLayout', ['android:layout_width="40dp"', 'android:layout_height="40dp"',
                                'android:background="@drawable/bg_icon_soft"'],
                [icon(ic, size='22dp', tint='@color/primary', extra=['android:layout_gravity="center"'])]))
    r.append(ll([
        tv(text=title, extra=['android:textColor="@color/text"', 'android:textSize="15.5sp"', 'android:textStyle="bold"']),
        tv(id=sub_id, extra=['android:textColor="@color/text_secondary"', 'android:textSize="13sp"',
                             'android:layout_marginTop="1dp"']),
    ], w='0dp', extra=['android:layout_weight="1"', 'android:layout_marginStart="14dp"']))
    if right == 'chevron':
        r.append(icon('chevron', size='20dp'))
    else:
        r.append(right)
    return ll(r, vertical=False, id=id, extra=[
        'android:gravity="center_vertical"', 'android:paddingStart="16dp"', 'android:paddingEnd="14dp"',
        'android:paddingTop="12dp"', 'android:paddingBottom="12dp"', 'android:background="?android:attr/selectableItemBackground"',
        'android:clickable="true"', 'android:focusable="true"'])


def group(rows):
    out = []
    for i, r in enumerate(rows):
        if i:
            out.append(divider())
        out.append(r)
    return ll(out, extra=['android:background="@drawable/section_bg"', 'android:paddingTop="4dp"',
                          'android:paddingBottom="4dp"', 'android:layout_marginBottom="6dp"'])


sw_auto = el('Switch', ['android:id="@+id/swAutoList"', 'android:layout_width="wrap_content"',
                        'android:layout_height="wrap_content"', 'android:contentDescription="Automatisch erkennen"'])

setup_card = ll([
    el('FrameLayout', ['android:id="@+id/setupIconBg"', 'android:layout_width="46dp"', 'android:layout_height="46dp"',
                       'android:background="@drawable/bg_icon_ok"'],
       [icon('check', id='setupIcon', size='24dp', tint='@color/ok', extra=['android:layout_gravity="center"'])]),
    ll([
        tv(id='tvSetupTitle', text='Alles eingerichtet', extra=['android:textColor="@color/text"', 'android:textSize="16sp"',
                                                                'android:textStyle="bold"']),
        tv(id='tvSetupSub', extra=['android:textColor="@color/text_secondary"', 'android:textSize="13sp"',
                                   'android:layout_marginTop="2dp"']),
    ], w='0dp', extra=['android:layout_weight="1"', 'android:layout_marginStart="14dp"']),
    icon('chevron', size='20dp'),
], vertical=False, id='rowSetup', extra=['android:background="@drawable/bg_round"', 'android:padding="18dp"',
                                         'android:gravity="center_vertical"', 'android:layout_marginBottom="8dp"',
                                         'android:clickable="true"', 'android:focusable="true"'])

settings_list = [
    ll([tv(text='Einstellungen', style='ScreenTitle')], extra=['android:paddingStart="4dp"', 'android:paddingTop="10dp"',
                                                               'android:paddingBottom="14dp"']),
    setup_card,
    tv(text='Erfassung', style='GroupLabel'),
    group([srow('rowOrt', 'pin', 'Arbeitsort', 'tvRowOrt'),
           srow('rowHome', 'house', 'Zuhause (Homeoffice)', 'tvRowHome'),
           srow('rowAuto', 'clock', 'Automatisch erkennen', 'tvRowAuto', right=sw_auto),
           srow('rowWege', 'car', 'Wege zur Stempeluhr', 'tvRowWege')]),
    tv(text='Grenzen &amp; Erinnerungen', style='GroupLabel'),
    group([srow('rowGrenzen', 'alert', 'Höchstarbeitszeit', 'tvRowGrenzen'),
           srow('rowWarn', 'bell', 'Warnungen', 'tvRowWarn')]),
    tv(text='Arbeitszeit &amp; Geld', style='GroupLabel'),
    group([srow('rowSoll', 'coffee', 'Soll &amp; Pause', 'tvRowSoll'),
           srow('rowOt', 'euro', 'Überstunden', 'tvRowOt'),
           srow('rowZuschlag', 'percent', 'Zuschläge', 'tvRowZuschlag'),
           srow('rowFeiertage', 'cal', 'Feiertage', 'tvRowFeiertage')]),
    tv(id='tvVersion', w='match_parent', extra=['android:gravity="center"', 'android:textColor="@color/text_secondary"',
                                                'android:textSize="12sp"', 'android:layout_marginTop="14dp"',
                                                'android:layout_marginBottom="24dp"']),
]
scroll_settings = scroll('scrollSettings', settings_list, gone=True)

# =====================================================================
# EINSTELLUNGEN – Detailseiten (immer nur eine sichtbar)
# =====================================================================


def zrow(lbl, id, unit, itype='number'):
    return ll([
        tv(text=lbl, w='0dp', extra=['android:layout_weight="1"', 'android:textColor="@color/text"', 'android:textSize="15sp"']),
        el('EditText', [f'android:id="@+id/{id}"', 'style="@style/Field"', 'android:layout_width="84dp"',
                        'android:gravity="end"', f'android:inputType="{itype}"']),
        tv(text=unit, w='26dp', extra=['android:textColor="@color/text_secondary"', 'android:textSize="15sp"',
                                       'android:layout_marginStart="6dp"']),
    ], vertical=False, extra=['android:gravity="center_vertical"', 'android:layout_marginTop="8dp"'])


sec_setup = section([
    tv(id='tvPerms', w='match_parent', extra=['android:lineSpacingExtra="6dp"', 'android:textColor="@color/text"',
                                              'android:textSize="15sp"']),
    space('8dp'),
    btn('btnPermLocation', '1. Standort &amp; Benachrichtigungen erlauben', extra=['android:layout_marginTop="6dp"']),
    btn('btnPermBackground', '2. Standort „Immer zulassen“', extra=['android:layout_marginTop="8dp"']),
    btn('btnPermBattery', '3. Akku-Optimierung &amp; Wecker', extra=['android:layout_marginTop="8dp"']),
    hint('Bei Samsung oder Xiaomi zusätzlich in den Android-Einstellungen die Akku-Einschränkungen für Zeitwächter ausschalten.'),
], id='secSetup', extra=['android:visibility="gone"'])

sec_ort = section([
    tv(id='tvGeofence', w='match_parent', extra=['android:textColor="@color/text_secondary"', 'android:textSize="13.5sp"',
                                                 'android:layout_marginBottom="6dp"']),
    btn('btnUseCurrent', 'Aktuellen Standort übernehmen', extra=['android:drawableStart="@drawable/ic_pin"',
                                                                 'android:layout_marginTop="6dp"']),
    btn('btnPasteLocation', 'Aus Zwischenablage einfügen (Google Maps)', extra=['android:layout_marginTop="8dp"']),
    lfield('Breitengrad (oder „48.49, 9.21“ einfügen)', 'etLat', 'text', 'z. B. 48.491234').replace(
        'android:inputType="text"', 'android:inputType="text"\n    android:imeOptions="actionNext"'),
    lfield('Längengrad', 'etLon', 'numberDecimal|numberSigned', 'z. B. 9.204321').replace(
        'android:inputType="numberDecimal|numberSigned"',
        'android:inputType="numberDecimal|numberSigned"\n    android:imeOptions="actionDone"'),
    ll([
        btn('btnOpenMap', 'In Karte prüfen', w='0dp', extra=['android:layout_weight="1"']),
        btn('btnSaveLocation', 'Übernehmen', 'BtnFilled', w='0dp', extra=['android:layout_weight="1"',
                                                                         'android:layout_marginStart="8dp"']),
    ], vertical=False, extra=['android:layout_marginTop="14dp"']),
], id='secOrt', extra=['android:visibility="gone"'])

sec_home = section([
    tv(id='tvHomeStatus', w='match_parent', extra=['android:textColor="@color/text_secondary"', 'android:textSize="13.5sp"',
                                                   'android:layout_marginBottom="6dp"', 'android:lineSpacingExtra="3dp"']),
    btn('btnHomeUseCurrent', 'Aktuellen Standort als Zuhause übernehmen', extra=['android:drawableStart="@drawable/ic_house"',
                                                                                 'android:layout_marginTop="6dp"']),
    btn('btnHomePaste', 'Aus Zwischenablage einfügen (Google Maps)', extra=['android:layout_marginTop="8dp"']),
    lfield('Breitengrad (oder „48.49, 9.21“ einfügen)', 'etHomeLat', 'text', 'z. B. 48.512345'),
    lfield('Längengrad', 'etHomeLon', 'numberDecimal|numberSigned', 'z. B. 9.198765'),
    lfield('Radius um Zuhause (Meter)', 'etHomeRadius', 'number'),
    ll([
        btn('btnHomeClear', 'Entfernen', w='0dp', extra=['android:layout_weight="1"']),
        btn('btnHomeSave', 'Übernehmen', 'BtnFilled', w='0dp', extra=['android:layout_weight="1"',
                                                                     'android:layout_marginStart="8dp"']),
    ], vertical=False, extra=['android:layout_marginTop="14dp"']),
    hint('Stempelst du von Hand ein, prüft die App kurz den Standort: im Bereich Zuhause wird der Einsatz als '
         'Homeoffice markiert, im Bereich Arbeit als Büro. Homeoffice wird nie automatisch ausgestempelt. '
         'Automatisch eingestempelt wird zu Hause nicht.'),
], id='secHome', extra=['android:visibility="gone"'])

sec_erkennung = section([
    el('Switch', ['android:id="@+id/swEnabled"', 'android:layout_width="match_parent"', 'android:layout_height="wrap_content"',
                  'android:text="Automatisch erkennen (Kommen &amp; Gehen)"', 'android:textColor="@color/text"',
                  'android:textSize="15sp"']),
    lfield('Morgens: Minuten vom Ankommen bis zum Einstempeln (Parken + Laufen)', 'etOffset', 'number'),
    lfield('Abends: Minuten vom Ausstempeln bis zum Wegfahren (Laufen zum Auto)', 'etOffsetOut', 'number'),
    lfield('Radius um den Arbeitsort (Meter, 100–300 empfohlen)', 'etRadius', 'number'),
    lfield('Kurz weg? Sitzung fortsetzen, wenn zurück innerhalb von (Minuten)', 'etResume', 'number'),
], id='secErkennung', extra=['android:visibility="gone"'])

sec_grenzen = section([
    lfield('Höchstarbeitszeit (Stunden, ohne Pause)', 'etMaxHours', 'numberDecimal'),
    lfield('Späteste Ausstempel-Uhrzeit (z. B. 20:45, leer = keine)', 'etLatestOut', 'time', '20:45'),
    hint('Es gilt immer die frühere Grenze: 10 h + Pause oder diese Uhrzeit. Beginnt ein Dienst erst danach '
         '(Nachtdienst), gilt nur die 10-h-Grenze.'),
    lfield('Warnungen (Minuten vorher, mit Komma getrennt)', 'etWarn', 'text', '30, 15, 5'),
], id='secGrenzen', extra=['android:visibility="gone"'])

sec_soll = section([
    lfield('Sollarbeitszeit pro Tag Mo–Fr (Stunden, z. B. 38,5 h ÷ 5 = 7,7)', 'etSoll', 'numberDecimal'),
    hint('Samstag, Sonntag und Feiertage haben keine Sollzeit – dort zählt jede Stunde als Überstunde.'),
    lfield('Pause wird abgezogen ab Anwesenheit von (Stunden)', 'etPauseAfter', 'numberDecimal'),
    lfield('Pause höchstens (Minuten)', 'etBreak', 'number'),
    hint('', id='tvPauseExample'),
], id='secSoll', extra=['android:visibility="gone"'])

sec_ot = section([
    lfield('Überstunden zum Start (Stunden, auch negativ, z. B. 35 oder -3)', 'etStartBal', 'numberDecimal|numberSigned'),
    btn('btnStartDate', '', extra=['android:layout_marginTop="10dp"']),
    lfield('Auszahlung am Monatsende ab Kontostand (Stunden)', 'etLimit', 'numberDecimal'),
    lfield('Stundenlohn brutto (€)', 'etRate', 'numberDecimal', 'z. B. 25,50'),
    hint('Am Monatsende wird alles über dem Kontostand ausgezahlt, der nächste Monat beginnt dann wieder mit diesem Wert.'),
], id='secOt', extra=['android:visibility="gone"'])

sec_zuschlag = section([
    zrow('Montag – Freitag', 'etSurWeek', '%'),
    zrow('Samstag – erste Stunden', 'etSurSatLow', '%'),
    zrow('… so viele Stunden', 'etSatLowHours', 'h', 'numberDecimal'),
    zrow('Samstag – danach', 'etSurSatHigh', '%'),
    zrow('Sonntag', 'etSurSun', '%'),
    zrow('Feiertag', 'etSurHol', '%'),
    hint('Werden Stunden ausgezahlt, zählt jede Stunde anteilig mit dem Zuschlag, mit dem sie in diesem Monat '
         'entstanden ist. Ein Feiertag geht vor Sonntag und Samstag.'),
], id='secZuschlag', extra=['android:visibility="gone"'])

sec_feiertage = section([
    label('Bundesland'),
    el('Spinner', ['android:id="@+id/spState"', 'android:layout_width="match_parent"', 'android:layout_height="wrap_content"',
                   'android:background="@drawable/bg_field"', 'android:padding="6dp"']),
    lfield('Zusätzliche Feiertage (TT.MM, mit Komma, z. B. 24.12, 31.12)', 'etExtraHol', 'text', 'leer lassen, wenn keine'),
    hint('', id='tvHolidayList'),
], id='secFeiertage', extra=['android:visibility="gone"'])

detail_header = ll([
    round_btn('btnBack', 'back', 'Zurück'),
    tv(id='tvDetailTitle', extra=['android:textColor="@color/text"', 'android:textSize="22sp"', 'android:textStyle="bold"',
                                  'android:layout_marginStart="12dp"']),
], vertical=False, extra=['android:gravity="center_vertical"', 'android:paddingTop="10dp"', 'android:paddingBottom="14dp"'])

scroll_detail = scroll('scrollDetail', [detail_header, sec_setup, sec_ort, sec_home, sec_erkennung, sec_grenzen, sec_soll, sec_ot,
                                        sec_zuschlag, sec_feiertage,
                                        btn('btnSaveAll', 'Speichern', 'BtnFilled', extra=['android:layout_marginTop="4dp"',
                                                                                           'android:layout_marginBottom="28dp"'])],
                       gone=True)

# =====================================================================
# NAVIGATION UNTEN
# =====================================================================


def nav_item(key, ic, text):
    return ll([
        el('FrameLayout', [f'android:id="@+id/nav{key}Pill"', 'android:layout_width="60dp"', 'android:layout_height="32dp"'],
           [icon(ic, id=f'nav{key}Icon', size='22dp', extra=['android:layout_gravity="center"'])]),
        tv(id=f'nav{key}Label', text=text, extra=['android:textSize="12sp"', 'android:textStyle="bold"',
                                                 'android:textColor="@color/text_secondary"', 'android:layout_marginTop="4dp"']),
    ], id=f'nav{key}', w='0dp', h='match_parent', extra=[
        'android:layout_weight="1"', 'android:gravity="center"', 'android:clickable="true"', 'android:focusable="true"',
        'android:background="?android:attr/selectableItemBackgroundBorderless"'])


nav = ll([nav_item('Heute', 'home', 'Heute'), nav_item('Zeit', 'chart', 'Arbeitszeit'),
          nav_item('Settings', 'gear', 'Einstellungen')], vertical=False, h='76dp', id='navBar',
         extra=['android:background="@color/nav_bg"', 'android:elevation="8dp"'])

frame = el('FrameLayout', ['android:layout_width="match_parent"', 'android:layout_height="0dp"', 'android:layout_weight="1"'],
           [scroll_heute, scroll_zeit, scroll_settings, scroll_detail, fab])

root = el('LinearLayout', [NS, 'android:layout_width="match_parent"', 'android:layout_height="match_parent"',
                           'android:background="@color/bg"', 'android:fitsSystemWindows="true"',
                           'android:orientation="vertical"'],
          [frame, el('View', ['android:layout_width="match_parent"', 'android:layout_height="1dp"',
                              'android:background="@color/line"']), nav])

out = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Zeitwächter 2.0 – erzeugt mit gen_layout.py -->\n' + root
import sys
open(sys.argv[1], 'w', encoding='utf-8').write(out)
print('ok', len(out.splitlines()), 'Zeilen')
