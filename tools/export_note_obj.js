const fs = require('fs'), path = require('path');
const KB = 'C:/Users/Lenovo/Desktop/鲸鲸的专属工作站/新东西';
const OUT = 'C:/Users/Lenovo/Documents/deepseek-harness/default-workspace/CreateFuelingImagination/src/main/resources/assets/fuelingimagination/models/block';

const rot = (p, o, d) => {
  let x = p[0]-o[0], y = p[1]-o[1], z = p[2]-o[2];
  const ax = d[0]*Math.PI/180, ay = d[1]*Math.PI/180, az = d[2]*Math.PI/180;
  if (ax) { const t=y*Math.cos(ax)-z*Math.sin(ax); z=y*Math.sin(ax)+z*Math.cos(ax); y=t; }
  if (ay) { const t=x*Math.cos(ay)+z*Math.sin(ay); z=-x*Math.sin(ay)+z*Math.cos(ay); x=t; }
  if (az) { const t=x*Math.cos(az)-y*Math.sin(az); y=x*Math.sin(az)+y*Math.cos(az); x=t; }
  return [x+o[0], y+o[1], z+o[2]];
};

function exportOne(file, name) {
  const m = JSON.parse(fs.readFileSync(file, 'utf8'));
  const byId = new Map();
  for (const e of m.elements || []) if (e.uuid) byId.set(e.uuid, e);
  const tw = m.resolution ? m.resolution.width : 16;
  const th = m.resolution ? m.resolution.height : 16;
  const faces = [];
  const stack = (m.outliner || []).map(n => ({ node: n, chain: [] })).reverse();
  while (stack.length) {
    const { node, chain } = stack.pop();
    if (typeof node === 'string') {
      const e = byId.get(node);
      if (!e || e.visibility === false || e.export === false) continue;
      const c = chain.slice();
      if (e.rotation) c.push([e.origin || [0,0,0], e.rotation]);
      const [x1,y1,z1] = e.from, [x2,y2,z2] = e.to;
      const quads = {
        north: [[x2,y1,z1],[x1,y1,z1],[x1,y2,z1],[x2,y2,z1]],
        south: [[x1,y1,z2],[x2,y1,z2],[x2,y2,z2],[x1,y2,z2]],
        east:  [[x2,y1,z2],[x2,y1,z1],[x2,y2,z1],[x2,y2,z2]],
        west:  [[x1,y1,z1],[x1,y1,z2],[x1,y2,z2],[x1,y2,z1]],
        up:    [[x1,y2,z2],[x2,y2,z2],[x2,y2,z1],[x1,y2,z1]],
        down:  [[x1,y1,z1],[x2,y1,z1],[x2,y1,z2],[x1,y1,z2]],
      };
      for (const dir of Object.keys(quads)) {
        const f = e.faces && e.faces[dir];
        if (!f || f.texture === null || f.texture === undefined) continue;
        const verts = quads[dir].map(v => { let cur = v.slice(); for (let k = c.length-1; k >= 0; k--) cur = rot(cur, c[k][0], c[k][1]); return cur; });
        faces.push({ verts, uv: f.uv, rot: f.rotation || 0 });
      }
      continue;
    }
    if (node.export === false || node.visibility === false) continue;
    const nc = chain.slice();
    if (node.rotation) nc.push([node.origin || [0,0,0], node.rotation]);
    const kids = node.children || [];
    for (let i = kids.length - 1; i >= 0; i--) stack.push({ node: kids[i], chain: nc });
  }
  const all = faces.flatMap(f => f.verts);
  const minX = Math.min(...all.map(v => v[0])), maxX = Math.max(...all.map(v => v[0]));
  const minY = Math.min(...all.map(v => v[1]));
  const minZ = Math.min(...all.map(v => v[2])), maxZ = Math.max(...all.map(v => v[2]));
  const cx = (minX+maxX)/2, cz = (minZ+maxZ)/2, fy = minY;
  const L = [`# Exported from ${path.basename(file)}`, `mtllib ${name}.mtl`, 'g note', 'usemtl texture0'];
  let vi = 1, ti = 1, ni = 1;
  for (const f of faces) {
    for (const v of f.verts) L.push(`v ${((8+(v[0]-cx))/16).toFixed(7)} ${((v[1]-fy)/16).toFixed(7)} ${((8+(v[2]-cz))/16).toFixed(7)}`);
    const [u1,v1,u2,v2] = f.uv;
    let pairs = [[u1,v2],[u2,v2],[u2,v1],[u1,v1]];
    const tn = ((Math.round(f.rot/90)) % 4 + 4) % 4;
    if (tn) pairs = pairs.slice(tn).concat(pairs.slice(0, tn));
    for (const [u,v] of pairs) L.push(`vt ${(u/tw).toFixed(7)} ${(v/th).toFixed(7)}`);
    const [a0,a1,a2] = f.verts;
    const ax=a1[0]-a0[0], ay=a1[1]-a0[1], az=a1[2]-a0[2];
    const bx=a2[0]-a0[0], by=a2[1]-a0[1], bz=a2[2]-a0[2];
    let nx=ay*bz-az*by, ny=az*bx-ax*bz, nz=ax*by-ay*bx;
    const len = Math.hypot(nx,ny,nz) || 1;
    L.push(`vn ${(nx/len).toFixed(7)} ${(ny/len).toFixed(7)} ${(nz/len).toFixed(7)}`);
    L.push(`f ${vi}/${ti}/${ni} ${vi+1}/${ti+1}/${ni} ${vi+2}/${ti+2}/${ni} ${vi+3}/${ti+3}/${ni}`);
    vi+=4; ti+=4; ni+=1;
  }
  fs.writeFileSync(path.join(OUT, name+'.obj'), L.join('\n')+'\n');
  fs.writeFileSync(path.join(OUT, name+'.mtl'), 'newmtl texture0\nKd 1 1 1\nmap_Kd #texture0\n');
  fs.writeFileSync(path.join(OUT, name+'.json'), JSON.stringify({
    parent: 'minecraft:block/block', loader: 'forge:obj',
    model: `fuelingimagination:models/block/${name}.obj`,
    flip_v: false, automatic_culling: false, render_type: 'minecraft:cutout',
    textures: { texture0: 'fuelingimagination:block/note', particle: 'fuelingimagination:block/note' }
  }, null, 2) + '\n');
  console.log(`  ${name}: ${faces.length} faces, X ${minX.toFixed(2)}..${maxX.toFixed(2)}, Y min ${minY.toFixed(2)}, tex ${tw}x${th}`);
}
exportOne(path.join(KB, 'YuanBenKong的笔记-关闭.bbmodel'), 'note_closed');
exportOne(path.join(KB, 'YuanBenKong的笔记-打开.bbmodel'), 'note_open');