const fs = require('fs'), path = require('path');
const SRC = 'C:/Users/Lenovo/Desktop/鲸鲸的专属工作站/新东西/装甲控制器/装甲控制器.json';
const OUT = 'C:/Users/Lenovo/Documents/deepseek-harness/default-workspace/CreateFuelingImagination/src/main/resources/assets/fuelingimagination/models/item';
const m = JSON.parse(fs.readFileSync(SRC, 'utf8'));
const rot = (p, o, d) => {
  let x = p[0]-o[0], y = p[1]-o[1], z = p[2]-o[2];
  const ax = d[0]*Math.PI/180, ay = d[1]*Math.PI/180, az = d[2]*Math.PI/180;
  if (ax) { const t=y*Math.cos(ax)-z*Math.sin(ax); z=y*Math.sin(ax)+z*Math.cos(ax); y=t; }
  if (ay) { const t=x*Math.cos(ay)+z*Math.sin(ay); z=-x*Math.sin(ay)+z*Math.cos(ay); x=t; }
  if (az) { const t=x*Math.cos(az)-y*Math.sin(az); y=x*Math.sin(az)+y*Math.cos(az); x=t; }
  return [x+o[0], y+o[1], z+o[2]];
};
const quads = {
  north: (x1,y1,z1,x2,y2,z2) => [[x2,y1,z1],[x1,y1,z1],[x1,y2,z1],[x2,y2,z1]],
  south: (x1,y1,z1,x2,y2,z2) => [[x1,y1,z2],[x2,y1,z2],[x2,y2,z2],[x1,y2,z2]],
  east:  (x1,y1,z1,x2,y2,z2) => [[x2,y1,z2],[x2,y1,z1],[x2,y2,z1],[x2,y2,z2]],
  west:  (x1,y1,z1,x2,y2,z2) => [[x1,y1,z1],[x1,y1,z2],[x1,y2,z2],[x1,y2,z1]],
  up:    (x1,y1,z1,x2,y2,z2) => [[x1,y2,z2],[x2,y2,z2],[x2,y2,z1],[x1,y2,z1]],
  down:  (x1,y1,z1,x2,y2,z2) => [[x1,y1,z1],[x2,y1,z1],[x2,y1,z2],[x1,y1,z2]],
};
const L = ['# armor_controller: element rotations baked into vertices (vanilla limit is +-45)',
           'mtllib armor_controller.mtl', 'g item', 'usemtl texture0'];
let vi = 1, ti = 1, ni = 1, count = 0;
for (const e of m.elements) {
  const [x1,y1,z1] = e.from, [x2,y2,z2] = e.to;
  let chain = [];
  if (e.rotation) {
    const d = [0,0,0];
    if (e.rotation.axis === 'x') d[0] = e.rotation.angle;
    if (e.rotation.axis === 'y') d[1] = e.rotation.angle;
    if (e.rotation.axis === 'z') d[2] = e.rotation.angle;
    if (d[0] || d[1] || d[2]) chain.push([e.rotation.origin || [0,0,0], d]);
  }
  for (const dir of Object.keys(quads)) {
    const f = e.faces && e.faces[dir];
    if (!f) continue;
    const verts = quads[dir](x1,y1,z1,x2,y2,z2).map(v => {
      let cur = v.slice();
      for (let k = chain.length-1; k >= 0; k--) cur = rot(cur, chain[k][0], chain[k][1]);
      return cur;
    });
    for (const [x,y,z] of verts) L.push(`v ${(x/16).toFixed(7)} ${(y/16).toFixed(7)} ${(z/16).toFixed(7)}`);
    const [u1,v1,u2,v2] = f.uv;
    let pairs = [[u1,v2],[u2,v2],[u2,v1],[u1,v1]];
    const tn = (((Math.round((f.rotation||0)/90)) % 4) + 4) % 4;
    if (tn) pairs = pairs.slice(tn).concat(pairs.slice(0, tn));
    for (const [u,v] of pairs) L.push(`vt ${(u/16).toFixed(7)} ${(v/16).toFixed(7)}`);
    const [a0,a1,a2] = verts;
    const ax=a1[0]-a0[0], ay=a1[1]-a0[1], az=a1[2]-a0[2];
    const bx=a2[0]-a0[0], by=a2[1]-a0[1], bz=a2[2]-a0[2];
    const nx=ay*bz-az*by, ny=az*bx-ax*bz, nz=ax*by-ay*bx;
    const len = Math.hypot(nx,ny,nz) || 1;
    L.push(`vn ${(nx/len).toFixed(7)} ${(ny/len).toFixed(7)} ${(nz/len).toFixed(7)}`);
    L.push(`f ${vi}/${ti}/${ni} ${vi+1}/${ti+1}/${ni} ${vi+2}/${ti+2}/${ni} ${vi+3}/${ti+3}/${ni}`);
    vi+=4; ti+=4; ni+=1; count++;
  }
}
fs.writeFileSync(path.join(OUT,'armor_controller.obj'), L.join('\n')+'\n');
fs.writeFileSync(path.join(OUT,'armor_controller.mtl'), 'newmtl texture0\nKd 1 1 1\nmap_Kd #texture0\n');
const model = { parent: 'minecraft:block/block', loader: 'forge:obj',
  model: 'fuelingimagination:models/item/armor_controller.obj',
  flip_v: false, automatic_culling: false,
  textures: { texture0: 'fuelingimagination:item/armor_controller', particle: 'fuelingimagination:item/armor_controller' } };
if (m.display) model.display = m.display;
fs.writeFileSync(path.join(OUT,'armor_controller.json'), JSON.stringify(model, null, 2) + '\n');
console.log(`  ${count} faces, ${m.elements.length} elements, rotations baked`);