/* WGS84 alignment import and chainage in metres; no uploaded coordinates leave the device. */
(function(root){
  const rad=Math.PI/180, earth=6371008.8;
  function distance(a,b){
    const dLat=(b[0]-a[0])*rad,dLon=(b[1]-a[1])*rad;
    const h=Math.sin(dLat/2)**2+Math.cos(a[0]*rad)*Math.cos(b[0]*rad)*Math.sin(dLon/2)**2;
    return 2*earth*Math.asin(Math.min(1,Math.sqrt(h)));
  }
  function check(points){
    if(!Array.isArray(points)||points.length<2||points.length>20000)throw Error('Ašiai reikia 2–20 000 taškų.');
    for(const p of points)if(!Array.isArray(p)||p.length<2||!Number.isFinite(p[0])||!Number.isFinite(p[1])||Math.abs(p[0])>90||Math.abs(p[1])>180)throw Error('Netinkamos WGS84 koordinatės (platuma, ilguma).');
    const metres=points.slice(1).reduce((sum,p,i)=>sum+distance(points[i],p),0);
    if(metres<1||metres>2000000)throw Error('Ašies ilgis turi būti nuo 1 m iki 2 000 km.');
    return points;
  }
  function parse(text,filename){
    if(/\.(geojson|json)$/i.test(filename)){
      const data=JSON.parse(text),g=data.type==='FeatureCollection'?data.features?.find(f=>f.geometry?.type==='LineString')?.geometry:data.type==='Feature'?data.geometry:data;
      if(g?.type!=='LineString')throw Error('GeoJSON turi turėti vieną LineString liniją.');
      return check(g.coordinates.map(p=>[Number(p[1]),Number(p[0])]));
    }
    if(/\.gpx$/i.test(filename)){
      const xml=new DOMParser().parseFromString(text,'application/xml');
      if(xml.querySelector('parsererror'))throw Error('Nepavyko perskaityti GPX.');
      const points=[...xml.getElementsByTagName('rtept')];
      if(!points.length)points.push(...xml.getElementsByTagName('trkpt'));
      return check(points.map(p=>[p.hasAttribute('lat')?Number(p.getAttribute('lat')):NaN,p.hasAttribute('lon')?Number(p.getAttribute('lon')):NaN]));
    }
    if(/\.csv$/i.test(filename)){
      const lines=text.trim().split(/\r?\n/).filter(Boolean);
      if(!lines.length)throw Error('CSV failas tuščias.');
      const separator=lines[0].includes(';')?';':',';
      const columns=lines.map(line=>line.split(separator).map(s=>s.trim().replace(/^"|"$/g,'')));
      const header=columns[0].map(s=>s.toLowerCase());
      const latitude=header.findIndex(s=>['lat','latitude','platuma'].includes(s));
      const longitude=header.findIndex(s=>['lon','lng','longitude','ilguma'].includes(s));
      if(latitude<0||longitude<0)throw Error('CSV antraštėje turi būti lat ir lon stulpeliai.');
      return check(columns.slice(1).map(row=>[row[latitude]?.length?Number(row[latitude]):NaN,row[longitude]?.length?Number(row[longitude]):NaN]));
    }
    throw Error('Naudok GeoJSON, GPX arba CSV failą.');
  }
  function prepare(points){
    check(points);const lengths=[0];
    for(let i=1;i<points.length;i++)lengths.push(lengths[i-1]+distance(points[i-1],points[i]));
    return {points,lengths,total:lengths[lengths.length-1]};
  }
  function nearest(line,point){
    let found=null;
    for(let i=1;i<line.points.length;i++){
      const a=line.points[i-1],b=line.points[i];
      const scale=Math.cos(((a[0]+b[0]+point[0])/3)*rad),k=earth*rad;
      const x=(b[1]-a[1])*scale*k,y=(b[0]-a[0])*k;
      const px=(point[1]-a[1])*scale*k,py=(point[0]-a[0])*k;
      const squared=x*x+y*y;if(squared<1e-12)continue;
      const t=Math.max(0,Math.min(1,(px*x+py*y)/squared));
      const cross=x*py-y*px,off=Math.hypot(px-t*x,py-t*y);
      if(!found||off<found.offset)found={offset:off,side:cross>0?'kairėje':cross<0?'dešinėje':'ant ašies',metres:line.lengths[i-1]+t*(line.lengths[i]-line.lengths[i-1]),point:[a[0]+t*(b[0]-a[0]),a[1]+t*(b[1]-a[1])]};
    }
    return found;
  }
  function at(line,metres){
    const d=Math.min(line.total,Math.max(0,metres));
    let i=1;while(i<line.lengths.length-1&&line.lengths[i]<d)i++;
    const span=line.lengths[i]-line.lengths[i-1],t=span?(d-line.lengths[i-1])/span:0;
    return line.points[i-1].map((n,j)=>n+t*(line.points[i][j]-n));
  }
  function station(metres){const v=Math.round(metres/5)*5;return Math.floor(v/100)+'+'+String(v%100).padStart(2,'0')}
  root.KurAsAlignment={parse,prepare,nearest,at,station};
})(typeof window==='undefined'?globalThis:window);
