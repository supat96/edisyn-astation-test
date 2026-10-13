package edisyn.synth.novationastation;
import edisyn.*; import javax.sound.midi.ShortMessage; import java.util.*; import java.lang.reflect.Field; import sun.misc.Unsafe;
public class AStationSubsystemTest {
    static int pass, fail; static String[] names; static Runnable[] bodies;
    public static void main(String[] a) { build(); for (int i=0;i<bodies.length;i++) {
        java.io.PrintStream oo=System.out, oe=System.err; java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(b)); System.setErr(new java.io.PrintStream(b)); Throwable t=null;
        try { bodies[i].run(); } catch (Throwable e) { t=e; } finally { System.setOut(oo); System.setErr(oe); }
        if (t==null) { pass++; System.out.println("ok "+(i+1)+" - "+names[i]); }
        else { fail++; System.out.println("not ok "+(i+1)+" - "+names[i]); System.out.println("# "+t.toString().replace("\n"," ")); } }
        System.out.println("# pass "+pass); System.out.println("# fail "+fail); System.exit(fail==0?0:1); }
    static ArrayList<Runnable> tb=new ArrayList<Runnable>(); static ArrayList<String> nb=new ArrayList<String>();
    static void test(String n, Runnable r){ nb.add(n); tb.add(r); }
    static void build(){ names=nb.toArray(new String[0]); bodies=tb.toArray(new Runnable[0]); }
    static void t(String n,Runnable r){ test(n,r); }
    static void ok(boolean c,String m){ if(!c) throw new AssertionError(m); }
    static void eqi(int a,int e,String m){ if(a!=e) throw new AssertionError(m+": expected <"+e+"> was <"+a+">"); }
    static void eq(Object a,Object e,String m){ if(!Objects.equals(a,e)) throw new AssertionError(m+": expected <"+e+"> was <"+a+">"); }
    static void eqb(byte[] a,byte[] e,String m){ if(!Arrays.equals(a,e)) throw new AssertionError(m+": expected <"+Arrays.toString(e)+"> was <"+Arrays.toString(a)+">"); }
    static void eqs(String[] a,String[] e,String m){ if(!Arrays.equals(a,e)) throw new AssertionError(m+": expected <"+Arrays.toString(e)+"> was <"+Arrays.toString(a)+">"); }
    static Throwable boom(Class<? extends Throwable> c,Runnable r){ try{r.run();}catch(Throwable t){ if(c.isInstance(t))return t; throw new AssertionError("expected "+c.getName()+" got "+t);} throw new AssertionError("expected "+c.getName()); }
    static void has(Throwable t,String s){ ok(t.getMessage()!=null&&t.getMessage().contains(s),"message must contain <"+s+"> but was <"+t.getMessage()+">"); }
    static Unsafe U; static { try{Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(Unsafe)f.get(null);}catch(Exception e){throw new RuntimeException(e);} }
    @SuppressWarnings("unchecked") static <X> X newx(Class<X> c){ try{return (X)U.allocateInstance(c);}catch(Exception e){throw new RuntimeException(e);} }
    static NovationAStation synth(){ NovationAStation s=newx(NovationAStation.class); s.setModel(new Model()); return s; }
    static NovationAStation synth(int bank,int patch){ NovationAStation s=synth(); s.getModel().set("bank",bank); s.getModel().set("number",patch); return s; }
    static byte[] req(int type){ return new byte[]{(byte)0xF0,0,0x20,0x29,1,0x40,0x7F,(byte)type,0,0,0,0,0,(byte)0xF7}; }
    static SysexMessage msg(SysexMessage.Type t){ return new SysexMessage.Builder(t).build(); }
    static byte[] dump(){ return new SysexMessage.Builder(SysexMessage.Type.PROGRAM_DUMP).build().getBytes(); }
    static byte[] dump(int bank,int num,int idx,int val){ return new SysexMessage.Builder(SysexMessage.Type.PROGRAM_DUMP).withProgramBank((byte)bank).withProgramNumber((byte)num).withPayload(idx,(byte)val).build().getBytes(); }
    static String pname(int b,int p){ NovationAStation s=synth(b,p); return s.getPatchName(s.getModel()); }
    static String nextName(int b,int p){ NovationAStation s=synth(b,p); return s.getPatchName(s.getNextPatchLocation(s.getModel())); }
    static int opt(java.util.OptionalInt o){ return o.isPresent()?o.getAsInt():-1; }

    static { t("boundaries", new Runnable(){ public void run(){
        String[][] B={{"NONE","0","127","0"},{"CENTRIC_24","52","76","64"},{"CENTRIC_100","14","114","64"},{"CENTRIC_127","0","127","64"},{"BOOLEAN","0","1","0"},{"ARP_NON_SYNC_RATES","0","127","-64"}};
        for(String[] r:B){ Boundary b=Boundaries.valueOf(r[0]); int mn=Integer.parseInt(r[1]),mx=Integer.parseInt(r[2]); eqi(b.getMin(),mn,r[0]+".min"); eqi(b.getMax(),mx,r[0]+".max"); eqi(b.getOffset(),Integer.parseInt(r[3]),r[0]+".offset");
            for(int v=-3;v<=130;v++) eq(b.validate(v),v>=mn&&v<=mx,r[0]+".validate("+v+")"); }
        Boundary d=new Boundary(){ public String[] getValues(){return null;} public int getMin(){return 5;} public int getMax(){return 9;} public int getOffset(){return 0;} };
        ok(d.validate(5)&&d.validate(9)&&!d.validate(4)&&!d.validate(10),"default validate inclusive");
        }}); }

    static { t("straight convertor", new Runnable(){ public void run(){
        Convertor c=Convertor.createStraight("foo",3,null,9,Boundaries.NONE); eqi(opt(c.getByteIndex()),3,"byteIndex"); ok(!c.getCC().isPresent(),"cc empty"); eqi(opt(c.getNRPN()),9,"nrpn"); ok(c.getBoundary()==Boundaries.NONE,"boundary identity");
        Convertor e=Convertor.createStraight("bar",null,7,null,null); ok(!e.getByteIndex().isPresent()&&!e.getNRPN().isPresent(),"null destinations empty"); eqi(opt(e.getCC()),7,"cc");
        Model m=new Model(); c.toModel(m,42); eqi(m.get("foo"),42,"toModel"); m.set("foo",7); eqi(c.toSynth(m),7,"toSynth");
        Convertor x=new Convertor(){ public void toModel(Model m,int v){} public int toSynth(Model m){return 0;} public java.util.OptionalInt getByteIndex(){return java.util.OptionalInt.empty();} public java.util.OptionalInt getCC(){return java.util.OptionalInt.empty();} public java.util.OptionalInt getNRPN(){return java.util.OptionalInt.empty();} };
        ok(x.getBoundary()==Boundaries.NONE,"default getBoundary"); }}); }

    static { t("mappings", new Runnable(){ public void run(){
        int n=0; HashMap<Integer,Convertor> ix=new HashMap<Integer,Convertor>(),cc=new HashMap<Integer,Convertor>(),np=new HashMap<Integer,Convertor>();
        for(Mappings m:Mappings.values()){ n++; eq(m.getKey(),m.name().replaceAll("[^A-Za-z0-9]","").toLowerCase(Locale.ROOT),"key "+m.name()); ok(m.getConvertor()!=null&&m.getBoundaries()!=null,"nonnull "+m.name());
            Convertor c=m.getConvertor(); ok(c.getByteIndex().isPresent()||c.getCC().isPresent()||c.getNRPN().isPresent(),"destination "+m.name());
            if(c.getByteIndex().isPresent()){int k=c.getByteIndex().getAsInt(); if(ix.containsKey(k)) ok(ix.get(k)==c,"dup index "+k); else ix.put(k,c);}
            if(c.getCC().isPresent()){int k=c.getCC().getAsInt(); if(cc.containsKey(k)) ok(cc.get(k)==c,"dup CC "+k); else cc.put(k,c);}
            if(c.getNRPN().isPresent()){int k=c.getNRPN().getAsInt(); if(np.containsKey(k)) ok(np.get(k)==c,"dup NRPN "+k); else np.put(k,c);} }
        ok(n>=20,"at least 20 mappings");
        String[][] S={{"PORTAMENTO_TIME","portamentotime","3","5","-1","NONE"},{"OSC1_SEMITONE","osc1semitone","7","40","-1","CENTRIC_24"},{"FILTER_KEY_TRACK","filterkeytrack","47","-1","11","NONE"},{"OSC3_FM_ENVELOPE_DEPTH","osc3fmenvelopedepth","57","-1","1","CENTRIC_127"},{"DEVICE_VOLUME","devicevolume","-1","7","-1","NONE"},{"BANK_SELECT","bankselect","-1","32","-1","NONE"}};
        for(String[] r:S){ Mappings m=Mappings.valueOf(r[0]); eq(m.getKey(),r[1],r[0]+" key"); eqi(opt(m.getConvertor().getByteIndex()),Integer.parseInt(r[2]),r[0]+" index"); eqi(opt(m.getConvertor().getCC()),Integer.parseInt(r[3]),r[0]+" cc"); eqi(opt(m.getConvertor().getNRPN()),Integer.parseInt(r[4]),r[0]+" nrpn"); eq(m.getBoundaries().name(),r[5],r[0]+" boundary"); }
        ok(Mappings.find("OSC%d_DETUNE",2)==Mappings.OSC2_DETUNE,"find OSC2_DETUNE"); ok(Mappings.find("OSC%d_SEMITONE",1)==Mappings.OSC1_SEMITONE,"find OSC1_SEMITONE");
        has(boom(IllegalArgumentException.class,new Runnable(){public void run(){Mappings.find("OSC%d_DETUNE",4);}}),"OSC4_DETUNE");
        ok(Convertors.getByKey("portamentotime").get()==Mappings.PORTAMENTO_TIME.getConvertor(),"byKey"); ok(!Convertors.getByKey("zzz").isPresent(),"unknown key");
        ok(Convertors.getByIndex(80).get()==Convertors.Packed.PACKED1,"idx80"); ok(Convertors.getByIndex(0).get()==Convertors.Packed.PACKED2,"idx0"); ok(Convertors.getByIndex(47).get()==Mappings.FILTER_KEY_TRACK.getConvertor(),"idx47"); ok(!Convertors.getByIndex(300).isPresent(),"idx300");
        ok(Convertors.getByCC(65).get()==Convertors.Packed.PACKED1,"cc65"); ok(Convertors.getByCC(67).get()==Convertors.Packed.PACKED2,"cc67"); ok(Convertors.getByCC(7).get()==Mappings.DEVICE_VOLUME.getConvertor(),"cc7"); ok(Convertors.getByCC(32).get()==Mappings.BANK_SELECT.getConvertor(),"cc32"); ok(!Convertors.getByCC(4).isPresent(),"cc4");
        ok(Convertors.getByNRPN(21).get()==Convertors.Packed.PACKED8,"np21"); ok(Convertors.getByNRPN(26).get()==Convertors.Packed.PACKED11,"np26"); ok(Convertors.getByNRPN(1).get()==Mappings.OSC3_FM_ENVELOPE_DEPTH.getConvertor(),"np1"); ok(!Convertors.getByNRPN(99).isPresent(),"np99"); }}); }

    static void pack(Convertor c,int v,String[] keys,int[] shifts,int[] masks){ Model m=new Model(); c.toModel(m,v); for(int i=0;i<keys.length;i++) eqi(m.get(keys[i]),(v>>shifts[i])&masks[i],c+" "+keys[i]); eqi(c.toSynth(m),v & mask(c),"toSynth "+c); }
    static int mask(Convertor c){ Model m=new Model(); c.toModel(m,127); return c.toSynth(m); }
    static { t("packed convertors", new Runnable(){ public void run(){
        for(Convertors.Packed p:Convertors.Packed.values()){ ok(p.getByteIndex().isPresent()||p.getCC().isPresent()||p.getNRPN().isPresent(),p+" destination"); ok(p.getBoundary()==Boundaries.NONE,p+" boundary"); int mk=mask(p); ok(mk!=0,p+" mask");
            for(int v:new int[]{0,1,2,7,8,13,63,64,100,127,128,200,255}){ Model m=new Model(); p.toModel(m,v); eqi(p.toSynth(m),v&mk,p+" roundtrip "+v); } }
        Convertor p1=Convertors.Packed.PACKED1; eqi(opt(p1.getByteIndex()),80,"p1 idx"); eqi(opt(p1.getCC()),65,"p1 cc"); ok(!p1.getNRPN().isPresent(),"p1 nrpn"); pack(p1,109,new String[]{"envelope1trigger","envelope2trigger","envelope3trigger","keysyncphase"},new int[]{0,1,2,3},new int[]{1,1,1,15});
        Convertor p2=Convertors.Packed.PACKED2; eqi(opt(p2.getByteIndex()),0,"p2 idx"); eqi(opt(p2.getCC()),67,"p2 cc"); pack(p2,53,new String[]{"unisonvoices","polyphonymode","filtertype"},new int[]{0,3,5},new int[]{7,3,1});
        Convertor p8=Convertors.Packed.PACKED8; eqi(opt(p8.getByteIndex()),121,"p8 idx"); eqi(opt(p8.getNRPN()),21,"p8 nrpn"); ok(!p8.getCC().isPresent(),"p8 cc"); pack(p8,13,new String[]{"reverbtype","chorustype"},new int[]{0,3},new int[]{7,1});
        Convertor p11=Convertors.Packed.PACKED11; eqi(opt(p11.getByteIndex()),127,"p11 idx"); eqi(opt(p11.getNRPN()),26,"p11 nrpn"); pack(p11,118,new String[]{"oscselect","mixerselect","pwmsource","lfoselect"},new int[]{0,2,4,6},new int[]{3,3,3,1}); }}); }

    static { t("sysex layout", new Runnable(){ public void run(){
        eqb(msg(SysexMessage.Type.CURRENT_PROGRAM_DUMP_REQUEST).getBytes(),req(0x40),"current request"); eqb(msg(SysexMessage.Type.PROGRAM_DUMP_REQUEST).getBytes(),req(0x41),"program request"); ok(msg(SysexMessage.Type.PROGRAM_DUMP_REQUEST).getType()==SysexMessage.Type.PROGRAM_DUMP_REQUEST,"request type"); eqi(msg(SysexMessage.Type.PROGRAM_DUMP_REQUEST).getPayload().length,0,"request payload");
        SysexMessage m=new SysexMessage.Builder(SysexMessage.Type.PROGRAM_DUMP).withControlByte((byte)1).withSoftwareVersion((byte)0x45).withVersionIncrement((byte)9).withProgramBank((byte)2).withProgramNumber((byte)42).withPayload(0,(byte)7).withPayload(127,(byte)100).build();
        byte[] b=m.getBytes(); eqi(b.length,142,"dump length"); eqi(b[7],1,"type"); eqi(b[8],1,"control"); eqi(b[9],0x45,"sw"); eqi(b[10],9,"inc"); eqi(b[11],2,"bank"); eqi(b[12],42,"number"); eqi(b[0]&0xFF,0xF0,"start"); eqi(b[b.length-1]&0xFF,0xF7,"end"); eqi(m.getPayload().length,128,"payload len"); eqi(m.getPayload()[0],7,"payload0"); eqi(m.getPayload()[127],100,"payload127");
        eqb(SysexMessage.parse(b).getBytes(),b,"roundtrip"); eqi(m.getSoftwareVersion(),0x45,"get sw"); eqi(m.getVersionIncrement(),9,"get inc"); eqi(m.getProgramBank(),2,"get bank"); eqi(m.getProgramNumber(),42,"get num"); eq(m.getFullVersion(),"8.5.09","version");
        eq(vmsg(0,0).getFullVersion(),"0.0.00","v0"); eq(vmsg(8,5).getFullVersion(),"1.0.05","v1"); eq(vmsg(127,99).getFullVersion(),"15.7.99","v2");
        byte[] p=m.getPayload(); p[0]=9; eqi(m.getPayload()[0],7,"payload copy"); eqi(b[13],7,"bytes isolated"); }}); }

    static SysexMessage vmsg(int sw,int inc){ return new SysexMessage.Builder(SysexMessage.Type.CURRENT_PROGRAM_DUMP_REQUEST).withSoftwareVersion((byte)sw).withVersionIncrement((byte)inc).build(); }
    static { t("sysex errors", new Runnable(){ public void run(){
        has(boom(IllegalStateException.class,new Runnable(){public void run(){SysexMessage.parse(Arrays.copyOf(dump(),141));}}),"Invalid message length for type PROGRAM_DUMP, expected 142, but was 141");
        byte[] s=dump(); s[2]=1; has(boom(IllegalStateException.class,new Runnable(){public void run(){SysexMessage.parse(s);}}),"Invalid start sequence");
        byte[] e=dump(); e[e.length-1]=0; has(boom(IllegalStateException.class,new Runnable(){public void run(){SysexMessage.parse(e);}}),"Invalid end sequence");
        has(boom(IllegalStateException.class,new Runnable(){public void run(){new SysexMessage.Builder(SysexMessage.Type.CURRENT_PROGRAM_DUMP_REQUEST).withPayload(0,(byte)1);}}),"payload out of boundary ! Type = CURRENT_PROGRAM_DUMP_REQUEST, index = 0");
        has(boom(IllegalStateException.class,new Runnable(){public void run(){new SysexMessage.Builder(SysexMessage.Type.PROGRAM_DUMP).withPayload(128,(byte)1);}}),"payload out of boundary ! Type = PROGRAM_DUMP, index = 128"); }}); }

    static { t("synth metadata", new Runnable(){ public void run(){
        NovationAStation s=synth(); eq(NovationAStation.getSynthName(),"Novation A Station","name"); eq(s.getDefaultResourceFileName(),"NovationAStation.init","init"); eq(s.getHTMLResourceFileName(),"NovationAStation.html","html");
        eqs(s.getBankNames(),new String[]{"1","2","3","4"},"banks"); String[] p=s.getPatchNumberNames(); eqi(p.length,100,"patches"); eq(p[0],"0","p0"); eq(p[99],"99","p99"); ok(s.getSupportsPatchWrites(),"writes"); ok(s.librarianTested(),"librarian"); eqi(s.getPauseAfterWritePatch(),50,"pause");
        eq(pname(2,7),"307","2/7"); eq(pname(0,0),"100","0/0"); eq(pname(3,99),"499","3/99"); eq(pname(0,9),"109","0/9");
        NovationAStation n=synth(-1,5); eq(n.getPatchName(n.getModel()),"Untitled","bank -1"); ok(n.getPatchLocationName(n.getModel())==null,"loc bank -1");
        NovationAStation n2=synth(2,-1); eq(n2.getPatchName(n2.getModel()),"Untitled","patch -1"); ok(n2.getPatchLocationName(n2.getModel())==null,"loc patch -1");
        Throwable t1=boom(IllegalStateException.class,new Runnable(){public void run(){s.getPatchName(synth(4,0).getModel());}}); eq(t1.getMessage(),"Invalid bank 4","bank msg");
        Throwable t2=boom(IllegalStateException.class,new Runnable(){public void run(){s.getPatchName(synth(0,100).getModel());}}); eq(t2.getMessage(),"Invalid patch 100","patch msg"); }}); }

    static { t("next patch location", new Runnable(){ public void run(){
        eq(nextName(0,0),"101","00"); eq(nextName(0,99),"200","099"); eq(nextName(2,57),"358","257"); eq(nextName(3,99),"100","399");
        NovationAStation s=synth(1,42); Model n=s.getNextPatchLocation(s.getModel()); eqi(n.get("bank"),1,"nb"); eqi(n.get("number"),43,"nn"); eq(n.getValue("name"),"243","nname");
        }}); }

    static void setParams(Model m){ m.set("osc1semitone",60); m.set("portamentotime",42); m.set("filterkeytrack",99); m.set("osc3fmenvelopedepth",50); m.set("polyphonymode",2); m.set("unisonvoices",5); m.set("filtertype",1); m.set("envelope1trigger",1); m.set("envelope2trigger",0); m.set("envelope3trigger",1); m.set("keysyncphase",9); }
    static Model params(){ Model m=new Model(); setParams(m); return m; }
    static { t("emit messages", new Runnable(){ public void run(){
        NovationAStation s=synth(2,7); setParams(s.getModel()); Model m=s.getModel();
        SysexMessage c=SysexMessage.parse(s.emit(m,true,false)); ok(c.getType()==SysexMessage.Type.CURRENT_PROGRAM_DUMP,"current type"); eqi(c.getProgramBank(),0,"current bank"); eqi(c.getProgramNumber(),0,"current num");
        byte[] p=c.getPayload(); eqi(p[3]&0xFF,42,"p3"); eqi(p[7]&0xFF,60,"p7"); eqi(p[47]&0xFF,99,"p47"); eqi(p[57]&0xFF,50,"p57"); eqi(p[0]&0xFF,53,"p0"); eqi(p[80]&0xFF,77,"p80");
        Model tmp=new Model(); tmp.set("bank",2); tmp.set("number",7); SysexMessage d=SysexMessage.parse(s.emit(tmp,false,false)); ok(d.getType()==SysexMessage.Type.PROGRAM_DUMP,"dump type"); eqi(d.getProgramBank(),3,"dump bank"); eqi(d.getProgramNumber(),7,"dump num"); eqi(d.getPayload()[7]&0xFF,60,"payload model");
        eqb(s.emit(tmp,false,true),s.emit(tmp,false,false),"toFile ignored");
        eqb(s.requestCurrentDump(),req(0x40),"current request"); SysexMessage r=SysexMessage.parse(s.requestDump(synth(0,5).getModel())); ok(r.getType()==SysexMessage.Type.PROGRAM_DUMP_REQUEST,"req type"); eqi(r.getProgramBank(),1,"req bank"); eqi(r.getProgramNumber(),5,"req num"); }}); }

    static { t("parse messages", new Runnable(){ public void run(){
        NovationAStation src=synth(2,7); setParams(src.getModel()); byte[] d=src.emit(src.getModel(),false,false); NovationAStation dst=synth(); eqi(dst.parse(d,false),Synth.PARSE_SUCCEEDED,"result"); Model t=dst.getModel();
        eqi(t.get("bank"),2,"bank"); eqi(t.get("number"),7,"number"); eq(t.getValue("name"),"307","name"); eqi(t.get("osc1semitone"),60,"semi"); eqi(t.get("portamentotime"),42,"port"); eqi(t.get("filterkeytrack"),99,"track"); eqi(t.get("osc3fmenvelopedepth"),50,"fm"); eqi(t.get("polyphonymode"),2,"poly"); eqi(t.get("unisonvoices"),5,"uni"); eqi(t.get("filtertype"),1,"filter"); eqi(t.get("envelope1trigger"),1,"e1"); eqi(t.get("envelope2trigger"),0,"e2"); eqi(t.get("envelope3trigger"),1,"e3"); eqi(t.get("keysyncphase"),9,"sync");
        NovationAStation b=synth(); b.getModel().set("osc1semitone",64); eqi(b.parse(dump(1,0,7,10),false),Synth.PARSE_SUCCEEDED,"parse low"); eqi(b.getModel().get("osc1semitone"),64,"reject low"); b.parse(dump(1,0,7,52),false); eqi(b.getModel().get("osc1semitone"),52,"accept min"); b.parse(dump(1,0,7,76),false); eqi(b.getModel().get("osc1semitone"),76,"accept max");
        eqi(b.parse(b.requestCurrentDump(),false),Synth.PARSE_IGNORE,"request ignored"); eqi(b.parse(new byte[]{1,2,3},false),Synth.PARSE_IGNORE,"garbage"); eqi(b.parse(new byte[0],false),Synth.PARSE_IGNORE,"empty"); }}); }

    static { t("cc nrpn emitAll", new Runnable(){ public void run(){
        NovationAStation s=synth(); Model m=s.getModel();
        s.handleSynthCCOrNRPN(new Midi.CCData(Midi.CCDATA_TYPE_RAW_CC,5,42,0,false)); eqi(m.get("portamentotime"),42,"raw cc");
        m.set("osc1semitone",64); s.handleSynthCCOrNRPN(new Midi.CCData(Midi.CCDATA_TYPE_RAW_CC,40,10,0,false)); eqi(m.get("osc1semitone"),64,"reject cc"); s.handleSynthCCOrNRPN(new Midi.CCData(Midi.CCDATA_TYPE_RAW_CC,40,76,0,false)); eqi(m.get("osc1semitone"),76,"accept cc");
        s.handleSynthCCOrNRPN(new Midi.CCData(Midi.CCDATA_TYPE_RAW_CC,67,53,0,false)); eqi(m.get("unisonvoices"),5,"p uni"); eqi(m.get("polyphonymode"),2,"p poly"); eqi(m.get("filtertype"),1,"p filt");
        s.handleSynthCCOrNRPN(new Midi.CCData(Midi.CCDATA_TYPE_NRPN,11,12345,0,false)); eqi(m.get("filterkeytrack"),96,"nrpn");
        m.set("portamentotime",42); Object[] cc=s.emitAll("portamentotime"); eqi(cc.length,1,"one cc"); ShortMessage q=(ShortMessage)cc[0]; eqi(q.getCommand(),ShortMessage.CONTROL_CHANGE,"cmd"); eqi(q.getData1(),5,"cc num"); eqi(q.getData2(),42,"cc val");
        m.set("filterkeytrack",96); Object[] np=s.emitAll("filterkeytrack"); eqi(np.length,4,"nrpn count"); eqi(((ShortMessage)np[0]).getData1(),99,"99"); eqi(((ShortMessage)np[1]).getData1(),98,"98"); eqi(((ShortMessage)np[2]).getData1(),6,"6"); eqi(((ShortMessage)np[2]).getData2(),96,"val");
        ok(s.emitAll("zzz")==null,"unknown null"); }}); }

    static { t("recognition registration", new Runnable(){ public void run(){
        ok(NovationAStationRec.recognize(dump()),"dump recognized"); ok(NovationAStationRec.recognize(msg(SysexMessage.Type.PROGRAM_DUMP_REQUEST).getBytes()),"request recognized"); ok(!NovationAStationRec.recognize(new byte[]{1,2,3}),"garbage"); byte[] b=dump(); b[b.length-1]=0; ok(!NovationAStationRec.recognize(b),"bad end");
        boolean found=false; for(String c:Synth.getClassNames()) if(c.equals("edisyn.synth.novationastation.NovationAStation")) found=true; ok(found,"registered"); }}); }
}
