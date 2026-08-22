/// After [scanner.scan]. Classifies the string, pulls fields, suggests actions.
/// Copy this file. Do not put this in the plugin — scan() stays a String.
class ScanHandled {
  const ScanHandled({
    required this.kind,
    required this.label,
    required this.raw,
    required this.fields,
    required this.actions,
  });

  final String kind;
  final String label;
  final String raw;
  final List<ScanField> fields;
  final List<ScanAction> actions;
}

class ScanField {
  const ScanField(this.k, this.v);
  final String k;
  final String v;
}

class ScanAction {
  const ScanAction(this.label, this.value);
  final String label;
  final String value;
}

ScanHandled handleScan(String raw) {
  final String v = raw.trim();
  if (v.isEmpty) {
    return _pack('text', 'TEXT', raw, const <ScanField>[]);
  }

  if (v.toUpperCase().startsWith('WIFI:')) {
    final String t = _after(v, 'T:') ?? '';
    final String s = _after(v, 'S:') ?? '';
    final String p = _after(v, 'P:') ?? '';
    return _pack('wifi', 'Wi-Fi', v, <ScanField>[
      ScanField('加密', t.isEmpty ? '—' : t),
      ScanField('SSID', s.isEmpty ? '—' : s),
      ScanField('密码', p.isEmpty ? '—' : p),
    ], extra: <ScanAction>[
      if (s.isNotEmpty) ScanAction('复制 SSID', s),
      if (p.isNotEmpty) ScanAction('复制密码', p),
    ]);
  }

  if (v.toUpperCase().startsWith('SMSTO:')) {
    final String rest = v.substring(6);
    final int i = rest.indexOf(':');
    final String to = i >= 0 ? rest.substring(0, i) : rest;
    final String body = i >= 0 ? rest.substring(i + 1) : '';
    return _pack('sms', '短信', v, <ScanField>[
      ScanField('号码', to),
      ScanField('内容', body),
    ], extra: <ScanAction>[
      if (to.isNotEmpty) ScanAction('复制号码', to),
    ]);
  }

  if (v.startsWith('wxp://') || v.contains('weixin.qq.com')) {
    return _pack('wechat', '微信支付', v, <ScanField>[ScanField('载荷', v)]);
  }

  if (v.contains('qr.alipay.com') || v.startsWith('alipay://') || v.startsWith('alipays://')) {
    return _pack('alipay', '支付宝', v, <ScanField>[ScanField('载荷', v)]);
  }

  if (v.startsWith('http://') || v.startsWith('https://')) {
    final Uri? uri = Uri.tryParse(v);
    return _pack('url', 'URL', v, <ScanField>[
      ScanField('地址', v),
      if (uri != null && uri.host.isNotEmpty) ScanField('主机', uri.host),
    ]);
  }

  if (v.contains('发血单')) {
    final String? no = RegExp(r'发血单号\s*([A-Z0-9-]+)').firstMatch(v)?.group(1);
    final String? abo = RegExp(r'(AB|[ABO])型').firstMatch(v)?.group(0);
    final String? rh = RegExp(r'RhD?\s*[+-]').firstMatch(v)?.group(0);
    final String? vol = RegExp(r'(\d+\s*(?:ml|U|u))').firstMatch(v)?.group(1);
    return _pack('slip', '发血单', v, <ScanField>[
      if (no != null) ScanField('单号', no),
      if (abo != null) ScanField('血型', abo),
      if (rh != null) ScanField('Rh', rh),
      if (vol != null) ScanField('剂量', vol),
    ], extra: <ScanAction>[
      if (no != null) ScanAction('复制单号', no),
    ]);
  }

  if (RegExp(r'^[=&>+]').hasMatch(v) && v.length >= 8) {
    return _pack('isbt', 'ISBT-128', v, <ScanField>[ScanField('符号', v)],
        extra: <ScanAction>[ScanAction('复制袋号', v)]);
  }

  if (RegExp(r'^[A-Z]\d{12}$').hasMatch(v)) {
    return _pack('din', '袋号', v, <ScanField>[
      ScanField('袋号', v),
      ScanField('血站', v.substring(1, 6)),
      ScanField('年', v.substring(6, 8)),
    ], extra: <ScanAction>[ScanAction('复制袋号', v)]);
  }

  if (v.startsWith(']C1') || v.startsWith(']e0') || RegExp(r'\(01\)\d{14}').hasMatch(v)) {
    final String? gtin = RegExp(r'\(01\)(\d{14})').firstMatch(v)?.group(1);
    return _pack('gs1', 'GS1', v, <ScanField>[
      ScanField('载荷', v),
      if (gtin != null) ScanField('GTIN', gtin),
    ], extra: <ScanAction>[
      if (gtin != null) ScanAction('复制 GTIN', gtin),
    ]);
  }

  if (RegExp(r'^\d{8}$|^\d{12,14}$').hasMatch(v)) {
    return _pack('ean', 'EAN/UPC', v, <ScanField>[ScanField('条码', v)],
        extra: <ScanAction>[ScanAction('复制条码', v)]);
  }

  return _pack('text', 'TEXT', v, <ScanField>[ScanField('内容', v)]);
}

ScanHandled _pack(
  String kind,
  String label,
  String raw,
  List<ScanField> fields, {
  List<ScanAction> extra = const <ScanAction>[],
}) {
  return ScanHandled(
    kind: kind,
    label: label,
    raw: raw,
    fields: fields,
    actions: <ScanAction>[...extra, ScanAction('复制原文', raw)],
  );
}

String? _after(String src, String key) {
  final int i = src.indexOf(key);
  if (i < 0) return null;
  final int start = i + key.length;
  final int end = src.indexOf(';', start);
  return src.substring(start, end < 0 ? src.length : end);
}
