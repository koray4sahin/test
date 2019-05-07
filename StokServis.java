/**
 * Copyright 2006 Tarım Kredi Kooperatifleri. Bütün hakları saklıdır
 */

package org.tarimkredi.ekoop.stok.service;


import com.barcodelib.barcode.QRCode;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.*;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.tarimkredi.ekoop.PanelStokTakipOzellik;
import org.tarimkredi.ekoop.borcalacak.domain.*;
import org.tarimkredi.ekoop.borcalacak.efatura.WebFaturaTipi;
import org.tarimkredi.ekoop.borcalacak.efatura.WebOlcuBirimKodu;
import org.tarimkredi.ekoop.borcalacak.efatura.WebSenaryoTipi;
import org.tarimkredi.ekoop.borcalacak.service.IBorcAlacakModulServis;
import org.tarimkredi.ekoop.common.*;
import org.tarimkredi.ekoop.common.domain.*;
import org.tarimkredi.ekoop.common.service.*;
import org.tarimkredi.ekoop.firmamusteri.domain.FirmaMusteri;
import org.tarimkredi.ekoop.firmamusteri.domain.FirmaMusteriDurumu;
import org.tarimkredi.ekoop.firmamusteri.domain.TuzelFirmaMusteri;
import org.tarimkredi.ekoop.firmamusteri.service.IFirmaMusteriModulServis;
import org.tarimkredi.ekoop.kart.alkazanws.KartWSSonuc;
import org.tarimkredi.ekoop.kart.alkazanws.PosKart;
import org.tarimkredi.ekoop.kart.alkazanws.Product;
import org.tarimkredi.ekoop.kart.restws.MobilKoop;
import org.tarimkredi.ekoop.krediyonetimi.domain.KrediTeskilatFaizOranTipi;
import org.tarimkredi.ekoop.krediyonetimi.domain.KrediTeskilatFaizOranlari;
import org.tarimkredi.ekoop.krediyonetimi.domain.KrediYonetimiStokIslemTipi;
import org.tarimkredi.ekoop.krediyonetimi.service.IKrediYonetimiModulServis;
import org.tarimkredi.ekoop.kullaniciyonetimi.domain.Kullanici;
import org.tarimkredi.ekoop.kullaniciyonetimi.service.Mailer;
import org.tarimkredi.ekoop.muhasebe.domain.*;
import org.tarimkredi.ekoop.muhasebe.service.IKaliciFisNoUretici;
import org.tarimkredi.ekoop.muhasebe.service.IMuhasebeModulServis;
import org.tarimkredi.ekoop.muhasebe.service.INumaraUretici;
import org.tarimkredi.ekoop.ortakkredi.OrtakKrediHatalar;
import org.tarimkredi.ekoop.ortakkredi.domain.*;
import org.tarimkredi.ekoop.ortakkredi.service.IOrtakKrediModulServis;
import org.tarimkredi.ekoop.ortaktakip.OrtakTakipHataKodu;
import org.tarimkredi.ekoop.ortaktakip.domain.GercekKisiOrtak;
import org.tarimkredi.ekoop.ortaktakip.domain.GercekTuzel;
import org.tarimkredi.ekoop.ortaktakip.domain.Ortak;
import org.tarimkredi.ekoop.ortaktakip.domain.TuzelKisiOrtak;
import org.tarimkredi.ekoop.ortaktakip.mernis.KPSClient.Client.tr.gov.nvi.kpsv2.model.KisiModel;
import org.tarimkredi.ekoop.ortaktakip.mernis.KPSClient.WebServiceClient.tr.gov.nvi.kpsv2.ws.client.exceptions.NviServiceException;
import org.tarimkredi.ekoop.ortaktakip.service.IOrtakTakipModulServis;
import org.tarimkredi.ekoop.personel.domain.KurumBirim;
import org.tarimkredi.ekoop.personel.domain.Personel;
import org.tarimkredi.ekoop.sevk.SevkHataKodu;
import org.tarimkredi.ekoop.sevk.domain.SevkStokCikisFis;
import org.tarimkredi.ekoop.sevk.service.GubretasOnayliCikisFisiOlustur;
import org.tarimkredi.ekoop.stok.StokHataKodu;
import org.tarimkredi.ekoop.stok.dao.IStokDao;
import org.tarimkredi.ekoop.stok.domain.*;
import org.tarimkredi.ekoop.stok.restServis.domain.Sonuc;
import org.tarimkredi.ekoop.stok.restServis.domain.StokRestServisSonuclar;
import org.tarimkredi.ekoop.teftis.domain.TempVeri;
import org.tarimkredi.ekoop.termintalep.domain.YemSiparis;

import javax.faces.model.SelectItem;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Boolean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.*;

public class StokServis extends BaseServis<IStokDao> implements IStokServis, IStokModulServis {
    private INumaraUretici geciciFisNoUretici;
    private IFirmaMusteriModulServis firmaMusteriModulServis;
    private IMuhasebeModulServis muhasebeModulServis;
    private ITakvim takvim;
    private IKaliciFisNoUretici<StokFisNoSayac> stokFisNoUretici;
    private ICommonServis commonServis;
    private IStokBakiyeHesaplayici bakiyeHesaplayici;
    private IOrtakKrediModulServis ortakKrediModulServis;
    private IOrtakTakipModulServis ortakTakipModulServis;
    private IKrediYonetimiModulServis krediYonetimiModulServis;
    private Mailer mailer;
    private IBorcAlacakModulServis borcAlacakModulServis;

    public StokServis(
            ISystemTime systemTime,
            IStokDao dao,
            ITakvim takvim,
            INumaraUretici geciciFisNoUretici,
            IKaliciFisNoUretici<StokFisNoSayac> stokFisNoUretici,
            IMuhasebeModulServis muhasebeModulServis,
            ICommonServis commonServis,
            IFirmaMusteriModulServis firmaMusteriModulServis,
            IOrtakTakipModulServis ortakTakipModulServis,
            IKrediYonetimiModulServis krediYonetimiModulServis,
            Mailer mailer) {
        this.takvim = takvim;
        this.stokFisNoUretici = stokFisNoUretici;
        this.geciciFisNoUretici = geciciFisNoUretici;
        this.firmaMusteriModulServis = firmaMusteriModulServis;
        this.firmaMusteriModulServis.setStokServis(this);
        this.systemTime = systemTime;
        this.dao = dao;
        this.muhasebeModulServis = muhasebeModulServis;
        this.commonServis = commonServis;
        this.bakiyeHesaplayici = dao;
        this.ortakTakipModulServis = ortakTakipModulServis;
        this.krediYonetimiModulServis = krediYonetimiModulServis;
        this.mailer = mailer;

    }

    public void setBakiyeHesaplayici(IStokBakiyeHesaplayici bakiyeHesaplayici) {
        this.bakiyeHesaplayici = bakiyeHesaplayici;
    }

    public ISystemTime getSystemTime() {
        return systemTime;
    }

    public void setBorcAlacakModulServis(IBorcAlacakModulServis borcAlacakModulServis) {
        this.borcAlacakModulServis = borcAlacakModulServis;
    }

    public ITakvim getTakvim() {
        return takvim;
    }

    public void setTakvim(ITakvim takvim) {
        this.takvim = takvim;
    }

    public void setOrtakKrediModulServis(IOrtakKrediModulServis ortakKrediModulServis) {
        this.ortakKrediModulServis = ortakKrediModulServis;
    }

    public void setOrtakTakipModulServis(IOrtakTakipModulServis ortakTakipModulServis) {
        this.ortakTakipModulServis = ortakTakipModulServis;
    }

//    public void setKrediYonetimiModulServis(IKrediYonetimiModulServis krediYonetimiModulServis) {
//        this.krediYonetimiModulServis = krediYonetimiModulServis;
//    }

    public void setStokFisNoUretici(IKaliciFisNoUretici<StokFisNoSayac> stokFisNoUretici) {
        this.stokFisNoUretici = stokFisNoUretici;
    }

    public void setGeciciFisNoUretici(INumaraUretici geciciFisNoUretici) {
        this.geciciFisNoUretici = geciciFisNoUretici;
    }

    public void stokGrupKaydet(StokGrup stokGrup) throws BusinessRuleException {
        validateStokGrup(stokGrup);
        kaydet(stokGrup);
    }

    public PanelStokTakipOzellik getPanelStokTakipOzellik() {
        return new PanelStokTakipOzellik(this);
    }

    private void validateStokGrup(StokGrup stokGrup) throws BusinessRuleException {
        stokGrup.validateAltSeviyeBilgisi();

        if (stokGrup.getAltSeviye()) {
            if (sorgula(new Sorgu(StokGrup.class, null, KriterFactory.esit("ustGrup.kod", stokGrup.getKod()))).size() > 0) {
                throw new BusinessRuleException(StokHataKodu.ALT_GRUBU_OLAN_STOK_GRUBU_ALT_SEVIYE_OLAMAZ);
            }
        }

        if (!stokGrup.getAltSeviye()) {
            if (sorgula(new Sorgu(Stok.class, null, KriterFactory.esit("grup.kod", stokGrup.getKod()))).size() > 0) {
                throw new BusinessRuleException(StokHataKodu.GRUBA_BAGLI_STOKLARIN_GRUP_BILGISI_DUZELTILMELIDIR);
            }
        }
    }

    public List<StokGrup> getStokGruplar(Sorgu sorgu) {

        boolean ustGrupSorgulaniyor = false;

        for (Kriter kriter : sorgu.getKriterler()) {
            if (kriter.getFieldName().equals("ustGrup.kod")) {
                ustGrupSorgulaniyor = true;
            }
        }

        List<StokGrup> listeGruplar = dao.sorgula(sorgu);

        if (!ustGrupSorgulaniyor) return listeGruplar;

        List<StokGrup> listeSonuc = new ArrayList<StokGrup>();

        listeSonuc.addAll(listeGruplar);

        Sorgu altGrupSorgu = new Sorgu(StokGrup.class);

        for (Kriter kriter : sorgu.getKriterler()) {
            altGrupSorgu.kriterEkle(kriter);
        }

        for (StokGrup grup : listeGruplar) {
            altGrupSorgu.kriterCikar("ustGrup.kod");
            altGrupSorgu.kriterEkle(KriterFactory.esit("ustGrup.kod", grup.getKod()));

            List<StokGrup> listeAltGruplar = sorgula(altGrupSorgu);

            for (StokGrup altGrup : listeAltGruplar) {
                Sorgu yeniSorgu = new Sorgu(StokGrup.class);

                for (Kriter kriter : sorgu.getKriterler()) {
                    if (kriter.getFieldName().equals("ustGrup.kod")) {
                        kriter = KriterFactory.esit("ustGrup.kod", altGrup.getKod());
                    }

                    yeniSorgu.kriterEkle(kriter);
                }

                listeSonuc.add(altGrup);
                listeSonuc.addAll(getStokGruplar(yeniSorgu));
            }
        }

        return listeSonuc;
    }

    public void stokKaydet(Stok stok) throws BusinessRuleException {
        if (stok.getFire() != null && stok.isTicariMal()) {
            throw new BusinessRuleException(StokHataKodu.SADECE_HAMMADDE_ICIN_FIRE_GIRILEBILIR);
        }

        if ((stok.getTakipOzellikTip() != TakipOzellikTip.YOK) && (stok.getUrunBilesenler().size() > 0)) {
            throw new BusinessRuleException(StokHataKodu.TAKIP_OZELLIGI_OLAN_STOKA_BILESEN_EKLEYEMEZSINIZ);
        }

        if (stok.getSatisIskontoTutari() != null && stok.getSatisIskontoYuzde() != null) {
            throw new BusinessRuleException(StokHataKodu.SATIS_ISKONTO_TUTAR_YADA_YUZDE_ALANLARINDAN_BIRISI_DOLU_OLMALIDIR);
        }

        if (stok.isTarimsalMekanizasyonKartMi()) {
            if (stok.getDnyRprBasTarihi() == null || stok.getDnyRprBitTarihi() == null) {
                throw new BusinessRuleException(StokHataKodu.DENEY_RAPOR_TARIHLERINI_KONTROL_EDINIZ);
            }
        }

        if (!stok.getIthalUrunMu()) {
            stok.setUretilenUlke(null);
            stok.setIthalEdilenUlke(null);
            stok.setUreticiFirmaninOrjinalAdi(null);
            stok.setUreticiFirmaninAdresi(null);
            stok.setTelefonu(null);
            stok.setWebAdresi(null);
            stok.setEmail(null);
            stok.setKontrolBelgeNo(null);
            stok.setKontrolBelgeTarihi(null);
            stok.setKontrolBelgeBitTarihi(null);
            stok.setKotaMiktari(null);
        }
        if (stok.getBelgeKarsilikStokMu() && !stok.getOnaylanmis()) {
            stok.setStokKodu(getStokKodu());
            stok.setOnaylanmis(true);
        }

        kaydet(stok);
    }


    public List<StokBasitKod> getKodlar(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<TicariSunumSekli> getTicariSunumKodlari(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<Birim> getBirimler(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<EtkinMaddeOran> getEtkinMaddeOranlar(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<EtkinMadde> getEtkinMaddeler(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<Stok> getStoklar(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<TicaretBorsasiKesintiOrani> getKurumMustahsilMakbuzuKesintiler(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<StokFisHareketDurdurmaKurali> getStokFisHareketIzinler(Sorgu sorgu) {
        return sorgula(sorgu);
    }

    public List<CikisIslemiDurdurmaKurali> getCikisIslemiDurdurmaKurallari(Sorgu sorgu) {
        return sorgula(sorgu);
    }

    public boolean stokUzerindeHareketVar(Stok stok) {
        return dao.stokUzerindeHareketVar(stok);
    }

    public void faturaFisHamaliyeBrutHesapla(FaturaFisHareket faturaFisHareket) throws BusinessRuleException {
        getFaturaFisYoneticisi().faturaFisHamaliyeBrutHesapla(faturaFisHareket);
    }

    public List<AkaryakitIade> getAkaryakitIadeler(Kooperatif kooperatif) {
        return dao.getAkaryakitIadeler(kooperatif);
    }

    public List<Stok> getgrubaGoreStoklar(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<PesinKrediliSatis> getPesinVeKrediliSatisFisleri(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<KurumStok> getKurumStokKartlari(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }


    public List<KdvOran> getKdvOranlari() {
        return muhasebeModulServis.getKdvOranlari();
    }

    public AcilisFis getAcilisFisi(Kurum kurum) {
        return dao.getAcilisFisi(kurum);
    }

    public void urunBilesenEkle(Stok stok, UrunBilesen urunBilesen) throws BusinessRuleException {
        stok.urunBilesenEkle(urunBilesen);
        stokKaydet(stok);
    }

    public List<Stok> getBileseniOlabilecekStoklar(Stok stok, List<Kriter> kriterler) {
        List<Stok> stoklar = getStoklar(new Sorgu(Stok.class, kriterler, null));

        stoklar.remove(stok);

        for (UrunBilesen urunBilesen : stok.getUrunBilesenler()) {
            stoklar.remove(urunBilesen.getBilesen());
        }

        return stoklar;
    }

    public void acilisFisiKes(AcilisFis fis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().acilisFisiKes(fis, kullanici);
    }

    public void acilisDuzeltmeFisiKes(AcilisDuzeltmeFis fis, Kullanici aktifKullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(fis, aktifKullanici);
    }

    public List<AcilisDuzeltmeFis> getAcilisDuzeltmeFisleri(Kurum kurum) {
        return dao.sorgula(new Sorgu(AcilisDuzeltmeFis.class, null, KriterFactory.esit("kurum", kurum)));
    }

    private String getKaliciFisNo(Tarih fisTarihi, Kurum kurum) throws BusinessRuleException {
        return getStokSayacYonetici().kaliciFisNoVer(fisTarihi, kurum);
    }

    private String getStokKodu() throws BusinessRuleException {
//        stokSayacTutarliMi();
        //todo:ilerde açılmışta kullanılmayan stok kart numaralrını ver ve o kullanılmayan stok kartını sil
        //gereksiz stok kartı açılmamış olur tedarik pazarlamaya sor.
        //select min(to_number(stok_kodu)) from stk_stok where id not in (select stok_id from stk_kurum_stok);
        return getStokSayacYonetici().stokKoduVer(commonServis.getMerkezBirligi());
    }

    public void stokSayacTutarliMi() {
        BigDecimal sayacSonStokKodu = dao.getDbMaxStokKodu("sonsayac");
        BigDecimal dbMaxStokKodu = dao.getDbMaxStokKodu("Max");
        if (!sayacSonStokKodu.equals(dbMaxStokKodu)) {
            dao.getStokSayacDuzelt();
        }

    }

    public void stokHareketEkle(StokFis stokFis, StokHareket stokHareket) throws BusinessRuleException {
        getStokFisYonetici().stokHareketEkle(stokFis, stokHareket);
    }

    public void stokHareketSil(StokFis stokFis, StokHareket stokHareket) throws BusinessRuleException {
        stokFis.hareketSil(stokHareket);
        kaydet(stokFis);
    }

    public List<Hesap> getStokAltHesaplar(Sorgu sorgu) {
        sorgu.kriterEkle(
                KriterFactory.or(
                        KriterFactory.benzer("hesapNo", HesapNolari.STOK_TICARI_MAL_HESABI),
                        KriterFactory.benzer("hesapNo", HesapNolari.STOK_HAMMADDE_HESABI)));

        return muhasebeModulServis.getTumKurumlaraAitAcikAltHesaplar(sorgu);
    }

    public List<Hesap> getTumKurumlaraAitAcikAltHesaplar(Sorgu sorgu) {
        return muhasebeModulServis.getTumKurumlaraAitAcikAltHesaplar(sorgu);
    }

    private StokSayacYoneticisi getStokSayacYonetici() {
        return new StokSayacYoneticisi(takvim, this, dao, geciciFisNoUretici, stokFisNoUretici);
    }

    private AkaryakitYoneticisi getAkaryakitYonetici() {
        return new AkaryakitYoneticisi(this, getStokSayacYonetici(), muhasebeModulServis, dao, takvim, ortakTakipModulServis, ortakKrediModulServis);
    }

    private StokFisYoneticisi getStokFisYonetici() {
        return new StokFisYoneticisi(this, getStokSayacYonetici(), muhasebeModulServis, dao, takvim, ortakTakipModulServis, ortakKrediModulServis);
    }

    private FaturaFisYoneticisi getFaturaFisYoneticisi() {
        return new FaturaFisYoneticisi(this, getStokSayacYonetici(), muhasebeModulServis, dao, takvim, ortakTakipModulServis, ortakKrediModulServis);
    }

    private MustahsilMahbuzuFisYoneticisi getMustahsilMahbuzuFisYoneticisi() {
        return new MustahsilMahbuzuFisYoneticisi(this, getStokSayacYonetici(), muhasebeModulServis, dao, takvim, ortakTakipModulServis, ortakKrediModulServis);
    }

    private KonsinyeFisYoneticisi getKonsinyeFisYonetici() {
        return new KonsinyeFisYoneticisi(this, getStokSayacYonetici(), muhasebeModulServis, dao, takvim, ortakTakipModulServis, ortakKrediModulServis);
    }

    private UretimFisYoneticisi getUretimFisYoneticisi() {
        return new UretimFisYoneticisi(this, getStokSayacYonetici(), muhasebeModulServis, dao, takvim, ortakTakipModulServis, ortakKrediModulServis);
    }

    public void giderMakbuzFisKes(GiderMakbuzuFis giderMakbuzuFis, Kullanici aktifKullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(giderMakbuzuFis, aktifKullanici);
    }

    public List<GiderMakbuzuFis> getGiderMakbuzFisler(Kurum aktifKurum) {
        return dao.listele(GiderMakbuzuFis.class);
    }

    public void mustahsilMakbuzuFisiKes(MustahsilMakbuzuFis mustahsilMakbuzuFis, Kullanici aktifKullanici) throws BusinessRuleException {

        if (mustahsilMakbuzuFis.getMuhatap().isOrtakMi()) {
            //sigorta modulu yapılınca ortagin sigorta borcu var ise onuda toplam borca dahil et.
            BigDecimal orkBorc = ortakKrediModulServis.getBorcBakiyeOrtaginVadesiGecenAcikSenetleri(mustahsilMakbuzuFis.getMuhatap().getOrtak(), mustahsilMakbuzuFis.getFisTarihi());
//            BigDecimal gecmisSermayeBorcu = ortakTakipModulServis.getGecmisSermayeBorcu(mustahsilMakbuzuFis.getMuhatap().getOrtak(), getSimdikiTarih());
            BigDecimal gecmisSermayeBorcu = mustahsilMakbuzuFis.getMuhatap().getOrtak().getSermaye().getVadesiGecenSermayeBorcu(mustahsilMakbuzuFis.getFisTarihi());
            BigDecimal toplamBorc = orkBorc.add(gecmisSermayeBorcu);
            if (EkoopUtils.isBuyuk(toplamBorc, BigDecimal.ZERO) && mustahsilMakbuzuFis.getAlimSekli().equals(UrunDegerlendirmeAlimSekli.NAKDEN)) {
                throw new BusinessRuleException(StokHataKodu.ORTAGIN_BORCU_VARKEN_NAKDEN_URUN_ALAMAZSINIZ, toplamBorc.toString());
            }

            if (EkoopUtils.isBuyuk(toplamBorc, BigDecimal.ZERO) && !EkoopUtils.isBuyuk(mustahsilMakbuzuFis.getBorcMahsupTutari() == null ? BigDecimal.ZERO : mustahsilMakbuzuFis.getBorcMahsupTutari(), BigDecimal.ZERO)) {
                throw new BusinessRuleException(StokHataKodu.BORC_MAHSUP_TUTARINI_GIRMELISINIZ);
            }

        }

        getMustahsilMahbuzuFisYoneticisi().stokFisKes(mustahsilMakbuzuFis, aktifKullanici);
    }

    public List<MustahsilMakbuzuFis> getMustahsilMakbuzuFisler(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void mustahsilMakbuzuHareketEkle(MustahsilMakbuzuFis mustahsilMakbuzuFis, MustahsilMakbuzHareket hareket) throws BusinessRuleException {
        getMustahsilMahbuzuFisYoneticisi().mustahsilMakbuzuHareketEkle(mustahsilMakbuzuFis, hareket);
    }

    public TicaretBorsasiKesintiOrani getTicaretBorsasiKesintiOrani(Kurum kurum) {
        return dao.getTicaretBorsasiKesintiOrani(kurum);
    }

    public Parametre getParametre(String parametreAdi, boolean degerRequired) throws BusinessRuleException {
        return commonServis.getParametre(parametreAdi, degerRequired);
    }

    public List<StokFis> getStokFisler(Sorgu sorgu) {
        return dao.getStokFisler(sorgu);
    }

    public void mustahsilMakbuzuFisOnayla(MustahsilMakbuzuFis mustahsilMakbuzuFis) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(mustahsilMakbuzuFis.getKurum(), "MustahsilFisOnayla", String.valueOf(mustahsilMakbuzuFis.getId()), true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }
        getMustahsilMahbuzuFisYoneticisi().mustahsilMakbuzuFisOnayla(mustahsilMakbuzuFis);
    }

    public List<GiderMakbuzuFis> getGiderMakbuzlari(Kurum kurum) {
        return dao.sorgula(new Sorgu(GiderMakbuzuFis.class, null, KriterFactory.esit("kurum", kurum)));
    }

    public void giderMakbuzFisOnayla(GiderMakbuzuFis giderMakbuzu) throws BusinessRuleException {
        getStokFisYonetici().giderMakbuzFisOnayla(giderMakbuzu);
    }

    public StokIptalFis mustahsilMakbuzuFisIptal(MustahsilMakbuzuFis mustahsilMakbuzuFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getMustahsilMahbuzuFisYoneticisi().mustahsilMakbuzuFisIptal(mustahsilMakbuzuFis, kullanici, kesilenFisler);
    }

    public void faturaFisKes(FaturaFis fis, Kullanici aktifKullanici) throws BusinessRuleException {
        getFaturaFisYoneticisi().faturaFisKes(fis, aktifKullanici);
    }

    public List<FaturaFis> getFaturaFisler(Kurum aktifKurum) {
        Sorgu sorgu = new Sorgu(FaturaFis.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", aktifKurum));
        return dao.sorgula(sorgu);
    }

    public void faturaFisOnayla(FaturaFis faturaFis) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(faturaFis.getKurum(), "FaturaFisOnayla", String.valueOf(faturaFis.getId()), true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }
        getFaturaFisYoneticisi().faturaFisOnayla(faturaFis);
    }

    public KdvOran getHamaliyeKdvOrani() throws BusinessRuleException {
        return muhasebeModulServis.getKdvOran(getParametre("HAMALIYE_KDV_ORANI", true).getBigDecimalDeger());
    }

    public KdvOran getNakliyeKdvOrani() throws BusinessRuleException {
        return muhasebeModulServis.getKdvOran(getParametre("NAKLIYE_KDV_ORANI", true).getBigDecimalDeger());
    }

    public void fazlalikFisHareketEkle(FazlalikFis fis, FazlalikFisHareket hareket, BigDecimal birimFiyat) throws BusinessRuleException {
        getStokFisYonetici().fazlalikFisHareketEkle(fis, hareket, birimFiyat);
    }

    public void fazlalikFisiKes(FazlalikFis fazlalikFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(fazlalikFis, kullanici);
    }

    public List<FazlalikFis> getFazlalikFisler(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void fazlalikFisOnayla(FazlalikFis fazlalikFis) throws BusinessRuleException {
        getStokFisYonetici().fazlalikFisOnayla(fazlalikFis);
    }

    public StokIptalFis fazlalikFisIptal(FazlalikFis fazlalikFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().fazlalikFisIptal(fazlalikFis, kullanici, kesilenFisler);
    }

    //todo:Yeni stok fis tipi eklendiginde gerekiyorsa StokFisTip.miktarsizTipler()'e eklenecek
    public BigDecimal getMevcutStokMiktari(KurumStok kurumStok) {
        return bakiyeHesaplayici.getMevcutStokMiktari(kurumStok);
    }

    public BigDecimal getMevcutKonsinyeStokMiktari(KurumStok kurumStok) {
        return bakiyeHesaplayici.getMevcutKonsinyeStokMiktari(kurumStok);
    }

    public BigDecimal getMevcutStokMiktari(KurumStok kurumStok, TakipOzelligi takipOzelligi) {
        if (kurumStok.isTakipOzelligiYok()) {
            return bakiyeHesaplayici.getMevcutStokMiktari(kurumStok);
        }

        return bakiyeHesaplayici.getMevcutStokMiktari(kurumStok, takipOzelligi);
    }

    public BigDecimal getMevcutStokTutari(KurumStok kurumStok) {
        return bakiyeHesaplayici.getMevcutStokTutari(kurumStok);
    }

    public BigDecimal getMevcutStokMiktariTarihli(KurumStok kurumStok, Tarih tarih) {
        return bakiyeHesaplayici.getMevcutStokMiktariTarihli(kurumStok, tarih);
    }

    public BigDecimal getMevcutStokTutariTarihli(KurumStok kurumStok, Tarih tarih) {
        return bakiyeHesaplayici.getMevcutStokTutariTarihli(kurumStok, tarih);
    }

    public List<StokIptalFis> getIptalFisler(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return sorgula(sorgu);
    }

    public void faturaFisHareketEkle(FaturaFis fis, FaturaFisHareket hareket) throws BusinessRuleException {
        getFaturaFisYoneticisi().faturaFisHareketEkle(fis, hareket);
    }

    public void faturaKdvBilgiSil(FaturaFis fis, FaturaKdvBilgi kdvBilgi) throws BusinessRuleException {
        fis.kdvBilgiSil(kdvBilgi);
        kaydet(fis);
    }

    public List<TuzelFirmaMusteri> getTuzelFirmaMusteriler(Kurum kurum) {
        return dao.sorgula(new Sorgu(TuzelFirmaMusteri.class, null, KriterFactory.esit("kurum", kurum)));
    }

    public void sigortaFisiKes(SigortaFis sigortaFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(sigortaFis, kullanici);
    }

    public void sigortaFisHareketEkle(SigortaFis fis, SigortaFisHareket hareket) throws BusinessRuleException {
        getStokFisYonetici().sigortaFisHareketEkle(fis, hareket);
    }

    public void faturaliNakliyeHamaliyeFisHareketEkle(FaturaliNakliyeHamaliyeFis fis, FaturaliNakliyeHamaliyeFisHareket hareket) throws BusinessRuleException {
        getStokFisYonetici().FaturaliNakliyeHamliyeFisHareketEkle(fis, hareket);
    }

    public List<SigortaFis> getSigortaFisler(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void sigortaFisOnayla(SigortaFis fis) throws BusinessRuleException {
        getStokFisYonetici().sigortaFisOnayla(fis);
    }

    public List<FaturaliNakliyeHamaliyeFis> getFaturaliNakliyeHamaliyeFisler(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void faturaliNakliyeHamaliyeFisKes(FaturaliNakliyeHamaliyeFis faturaliNakliyeHamaliyeFis, Kullanici kullanici) throws BusinessRuleException {
        getFaturaFisYoneticisi().stokFisKes(faturaliNakliyeHamaliyeFis, kullanici);
    }

    public void faturaliNakliyeHamaliyeFisiOnayla(FaturaliNakliyeHamaliyeFis fis) throws BusinessRuleException {
        getFaturaFisYoneticisi().faturaliNakliyeHamaliyeFisiOnayla(fis);
    }

    public StokIptalFis faturaliNakliyeHamaliyeFisIptal(FaturaliNakliyeHamaliyeFis faturaliNakliyeHamaliyeFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getFaturaFisYoneticisi().faturaliNakliyeHamaliyeFisIptal(faturaliNakliyeHamaliyeFis, kullanici, kesilenFisler);
    }

    public void alisFiyatArttirimFisKes(AlisFiyatArttirimFis fiyatArttirimFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(fiyatArttirimFis, kullanici);
    }

    public void alisFiyatIndirimFisKes(AlisFiyatIndirimFis fiyatIndirimFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(fiyatIndirimFis, kullanici);
    }

    public void fiyatArttirimFisHareketEkle(FiyatArttirimFis arttirimFis, FiyatArttirimFisHareket hareket) throws BusinessRuleException {
        getStokFisYonetici().fiyatArttirimFisHareketEkle(arttirimFis, hareket);
    }

    public void alisFiyatIndirimFisHareketEkle(AlisFiyatIndirimFis indirimFis, AlisFiyatIndirimFisHareket hareket) throws BusinessRuleException {
        getStokFisYonetici().fiyatIndirimFisHareketEkle(indirimFis, hareket);
    }

    public void giderMakbuzuHareketEkle(GiderMakbuzuFis giderMakbuzu, GiderMakbuzHareket hareket) throws BusinessRuleException {
        if (giderMakbuzu.getKurum().isKooperatif()) {
            validateMiktarTutarGiderKontrol(hareket.getStok());
        }
        getStokFisYonetici().giderMakbuzuHareketEkle(giderMakbuzu, hareket);
    }

    public void alisFiyatArttirimFisOnayla(AlisFiyatArttirimFis alisFiyatArttirimFis) throws BusinessRuleException {
        getStokFisYonetici().alisFiyatArttirimFisOnayla(alisFiyatArttirimFis);
    }

    public void alisFiyatIndirimFisOnayla(AlisFiyatIndirimFis alisFiyatIndirimFis) throws BusinessRuleException {
        getStokFisYonetici().alisFiyatIndirimFisOnayla(alisFiyatIndirimFis);
    }

    public StokFIFAIptalFis alisFiyatArttirimFisIptal(AlisFiyatArttirimFis alisFiyatArttirimFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().alisFiyatArttirimFisIptal(alisFiyatArttirimFis, kullanici, kesilenFisler);
    }

    public List<TuzelFirmaMusteri> getTuzelFirmaMusteriler(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public StokIptalFis sigortaFisIptal(SigortaFis sigortaFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().sigortaFisIptal(sigortaFis, kullanici, kesilenFisler);
    }

    public StokIptalFis faturaFisIptal(FaturaFis faturaFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(faturaFis.getKurum(), "FaturaFisIptal", String.valueOf(faturaFis.getId()), true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }
        return getFaturaFisYoneticisi().faturaFisIptal(faturaFis, kullanici, kesilenFisler);
    }

    public StokIptalFis giderMakbuzFisIptal(GiderMakbuzuFis giderMakbuzFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().giderMakbuzFisIptal(giderMakbuzFis, kullanici, kesilenFisler);
    }

    public List<KurumStok> getHammaddeStoklari(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.benzer("stok.stokMuhasebeHesabi.hesapNo", Stok.HAMMADDE_HESAP_PREFIX));
        return getKurumStokKartlari(kurum, sorgu);
    }

    public void stokHareketGuncelle(StokHareket hareket) throws BusinessRuleException {
        getStokFisYonetici().stokHarekGuncelle(hareket);
    }

    public void masrafaCikisFisKes(MasrafaCikisFis masrafaCikisFis, Kullanici aktifKullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(masrafaCikisFis, aktifKullanici);
    }

    public List getMasrafaCikisFisler(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void masrafaCikisFisiOnayla(MasrafaCikisFis masrafaCikisFis) throws BusinessRuleException {
        getStokFisYonetici().masrafaCikisFisiOnayla(masrafaCikisFis);
    }

    public StokIptalFis masrafaCikisFisIptal(MasrafaCikisFis masrafaCikisFis, Kullanici aktifKullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().masrafaCikisFisIptal(masrafaCikisFis, aktifKullanici, kesilenFisler);
    }

    public void noksanlikFisiKes(NoksanlikFis noksanlikFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(noksanlikFis, kullanici);
    }

    public void noksanlikFisHareketEkle(NoksanlikFis noksanlikFis, NoksanlikFisHareket hareket) throws BusinessRuleException {
        getStokFisYonetici().noksanlikFisHareketEkle(noksanlikFis, hareket);
    }

    public void noksanlikFisOnayla(NoksanlikFis fis) throws BusinessRuleException {
        getStokFisYonetici().noksanlikFisOnayla(fis);
    }

    public void masrafaCikisHareketEkle(MasrafaCikisFis cikisFis, MasrafaCikisFisHareket cikisFisHareket) throws BusinessRuleException {
        getStokFisYonetici().masrafaCikisHareketEkle(cikisFis, cikisFisHareket);
    }

    public List<Hesap> getMasrafYeriHesaplar(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.or(
                KriterFactory.benzer("hesapNo", "770"), KriterFactory.or(
                        KriterFactory.benzer("hesapNo", "760"), KriterFactory.or(
                                KriterFactory.esit("hesapNo", "61101000100001"), KriterFactory.or(
                                        KriterFactory.benzer("hesapNo", "62150"), KriterFactory.or(
                                                KriterFactory.benzer("hesapNo", "62170"), KriterFactory.or(
                                                        KriterFactory.benzer("hesapNo", "659"), KriterFactory.benzer("hesapNo", "25"))))))));

        return muhasebeModulServis.getKurumaAitAcikAltHesaplar(kurum, sorgu);
    }

    public StokIptalFis noksanlikFisIptal(NoksanlikFis noksanlikFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().noksanlikFisIptal(noksanlikFis, kullanici, kesilenFisler);
    }

    public MiktarsizTutarDuzeltmeFis miktarsizTutarDuzeltmeFisiKes(Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().miktarsizTutarDuzeltmeFisiKes(kullanici, kesilenFisler);
    }

    public void alistanIadeCikisFisiKes(AlistanIadeCikisFis alistanIadeFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(alistanIadeFis, kullanici);
    }

    public void alistanIadeFisCikisHareketEkle(AlistanIadeCikisFis alistanIadeFis, AlistanIadeCikisFisHareket hareket) throws BusinessRuleException {
        getStokFisYonetici().alistanIadeFisCikisHareketEkle(alistanIadeFis, hareket);
    }

    public List getAlistanIadeCikisFisler(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void alistanIadeCikisFisiOnayla(AlistanIadeCikisFis alistanIadeFis) throws BusinessRuleException {
        getStokFisYonetici().alistanIadeCikisFisiOnayla(alistanIadeFis);
    }

    public StokIptalFis alistanIadeCikisFisIptal(AlistanIadeCikisFis alistanIadeFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().alistanIadeCikisFisIptal(alistanIadeFis, kullanici, kesilenFisler);
    }

    public List<StokTakipOzellikDurum> takipOzelligineGoreStoklar(Sorgu sorgu, Kurum kurum) {
        return dao.takipOzelligineGoreStoklar(sorgu, kurum);
    }

    public List<StokTakipOzellikDurum> takipOzelligineGoreStoklar(Sorgu sorgu, String ekHql, Object[] ekParametreler) {
        return dao.takipOzelligineGoreStoklar(sorgu, ekHql, ekParametreler);
    }

    public List<Stok> grubaGoreStokSorgula(StokGrup grup, List<Kriter> kriterler) {
        return dao.grubaGoreStokSorgula(grup, kriterler);
    }

    public List<StokTakipOzellikDurum> getFireCikisFisiHammaddeStoklari(Sorgu sorgu, Kurum kurum) {
        List<StokTakipOzellikDurum> hammaddeStoklar = new ArrayList<StokTakipOzellikDurum>();
        List<StokTakipOzellikDurum> stoklar = dao.takipOzelligineGoreStoklar(sorgu, kurum);
        for (StokTakipOzellikDurum stokTakipOzellikDurum : stoklar) {
            if (stokTakipOzellikDurum.getKurumStok().getStok().getStokMuhasebeHesabi().getNo().getKebirNo().equals(Stok.HAMMADDE_HESAP_PREFIX)) {
                hammaddeStoklar.add(stokTakipOzellikDurum);
            }
        }
        return hammaddeStoklar;
    }

    public List<AlisFis> getAlisStokFisler(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public List<StokFis> getIptalEdilmemisOnayliAlisFisleri(Sorgu sorgu, Kurum kurum) {
        //metin bey tarafından kooperatiflerde yil kontrolu kaldirilid..06/2013
        //talepno 383981 nolu talebe gore tum kurumlar için eklendi.
//        if (!kurum.isKooperatif()) {
//            sorgu.kriterEkle(KriterFactory.esit("yil", getSimdikiTarih().getYil()));
//        }

        sorgu.kriterEkle(KriterFactory.bos("iptalEdenFis"));
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        sorgu.kriterEkle(KriterFactory.or(KriterFactory.esit("tip", StokFisTip.FATURA),
                KriterFactory.or(KriterFactory.esit("tip", StokFisTip.MUSTAHSIL_MAKBUZU), KriterFactory.esit("tip", StokFisTip.KONSINYE_GIRIS))));


        return sorgula(sorgu);

//        return dao.getIptalEdilmemisOnayliAlisFisleri(sorgu, kurum);
    }

    public BigDecimal getAlisFisindeKalanStokMiktari(AlisFis alisFis, KurumStok kurumStok) {
        return getStokFisYonetici().getAlisFisindeKalanStokMiktari(alisFis, kurumStok);
    }

    public BigDecimal getSatisFisindeKalanStokMiktari(SatisFis satisFis, KurumStok kurumStok) {
        return getStokFisYonetici().getSatisFisindeKalanStokMiktari(satisFis, kurumStok);
    }

    public void mustahsilMakbuzKesintiHesapla(MustahsilMakbuzuFis stokFis, MustahsilMakbuzHareket stokHareket) throws BusinessRuleException {
        getMustahsilMahbuzuFisYoneticisi().mustahsilMakbuzKesintileriHesapla(stokFis, stokHareket);
    }

    public List<KurumStok> getFistekiKurumStoklar(Sorgu sorgu, StokFis stokFis) {
        return dao.getFistekiKurumStoklar(sorgu, stokFis);
    }

    public void satisFisiKes(SatisFis satisFis, Kullanici kullanici, Tarih simdikiTarih) throws BusinessRuleException {
        getSatisFisYonetici().satisFisiKes(satisFis, kullanici);

//        if( satisFis instanceof KoopSatisFis &&  ((KoopSatisFis) satisFis).getOdemeSekli().equals( KoopSatisOdemeSekli.VERESIYE) ){
//            VeresiyeIslem veresiyeIslem = new VeresiyeIslem();
//
//            veresiyeIslem.setAlacak( ((KoopSatisFis) satisFis).getSumMOTTutar() );
//            veresiyeIslem.setBorc( BigDecimal.ZERO );
//            veresiyeIslem.setKurum( (Kooperatif) kullanici.getAktifKurum() );
//            veresiyeIslem.setIslemTarihi( getSimdikiTarih() );
//            veresiyeIslem.setValorTarihi( getSimdikiTarih().gunEkle(1));
//            veresiyeIslem.setStokFis( (KoopSatisFis)satisFis);
//            veresiyeIslem.setVeresiyeIslemTipi( VeresiyeIslemTipi.STOK_FATURA_DUZENLEME);
//            //TODO mahsuben tahsilat varsa onu da mahsuben tahsilat satırı olarak atalım
//            if( ( ((KoopSatisFis) satisFis).getMahsubenTahsilatTutar().compareTo( BigDecimal.ZERO) > 0)){
//
//                VeresiyeIslem veresiyeIslemMahsuben = new VeresiyeIslem();
//
//                veresiyeIslem.setAlacak( ((KoopSatisFis) satisFis).getMahsubenTahsilatTutar() );
//                veresiyeIslem.setBorc( BigDecimal.ZERO );
//                veresiyeIslem.setKurum( (Kooperatif) kullanici.getAktifKurum() );
//                veresiyeIslem.setIslemTarihi( getSimdikiTarih() );
//                veresiyeIslem.setValorTarihi( getSimdikiTarih().gunEkle(1));
//                veresiyeIslem.setStokFis( (KoopSatisFis)satisFis);
//                veresiyeIslem.setVeresiyeIslemTipi( VeresiyeIslemTipi.TAHSILAT);
//                veresiyeIslem.setTahsilatTipi( TahsilatTipi.MAHSUBEN );
////            veresiyeIslem.setMahsupHesap( secilenHesap.getHesap( servis ) );
//
//            }
////
//        }
    }

    private SatisFisYoneticisi getSatisFisYonetici() {
        return new SatisFisYoneticisi(this, getStokSayacYonetici(), muhasebeModulServis, dao, takvim, ortakTakipModulServis, ortakKrediModulServis);
    }

    public void satisFisHareketEkle(SatisFis satisFis, SatisFisHareket satisFisHareket) throws BusinessRuleException {


        if (!satisFisHareket.getStok().isTakipOzelligiYok()) {
            validateTakipOzellik(satisFis, satisFisHareket);
        }
        //talep no 41022 ye gore kontrol kaldirildi..
//        if (satisFisHareket.getStok().getStok().isTarimsalMekanizasyonKartMi()) {
//            if(satisFis.getFisTarihi().after(satisFisHareket.getStok().getStok().getDnyRprBitTarihi())){
//                throw new BusinessRuleException(StokHataKodu.STOK_KARTININ_DENEY_RAPOR_TARIHI_DOLMUSTUR);
//            }
//        }

        if (satisFis.getKurum().isMerkez() && satisFis.getHareketler().size() > 19) {
            throw new BusinessRuleException(StokHataKodu.SATIS_FISLERINDE_ALTI_ADET_STOK_HAREKETI_YAPABILIRSINIZ);
        } else if ((!satisFis.getKurum().isMerkez() && satisFis.getHareketler().size() > 19) && !satisFis.getGencCiftciMi()) {
            throw new BusinessRuleException(StokHataKodu.SATIS_FISLERINDE_ALTI_ADET_STOK_HAREKETI_YAPABILIRSINIZ);
        } else if ((!satisFis.getKurum().isMerkez() && satisFis.getHareketler().size() > 22) && satisFis.getGencCiftciMi()) {
            throw new BusinessRuleException(StokHataKodu.SATIS_FISLERINDE_ALTI_ADET_STOK_HAREKETI_YAPABILIRSINIZ);
        }


        if (satisFisHareket.getStok().getStok().getStokMuhasebeHesabi().getHesapNo().substring(0, 5).equals("15070")) {
            throw new BusinessRuleException(StokHataKodu.BU_STOK_SATISI_BURDAN_YAPILMAZ);
        }
        if (satisFis.getGercekKisiMi() && satisFisHareket.getStok().isAkaryakit()) {
            throw new BusinessRuleException(StokHataKodu.BU_STOK_SATISI_BURDAN_YAPILMAZ);
        }

//        if (satisFisHareket.getStok().isKulakKupeNumarali() && EkoopUtils.isBuyuk(satisFisHareket.getMiktar(), BigDecimal.ONE)) {
//            throw new BusinessRuleException(StokHataKodu.MIKTAR_HATALI_KONTROL_EDINIZ);
//        }

        //veresiye için limit kontrolleri
//        if (satisFis instanceof KoopSatisFis) {//(ismail)
//            KoopSatisFis koopSatisFis = (KoopSatisFis) satisFis;
//            if (koopSatisFis.getOdemeSekli().equals(KoopSatisOdemeSekli.VERESIYE) && koopSatisFis.getMuhatapTip().equals(MuhatapTip.ORTAK_ICI)) {
//                validateOrtakVeresiyeKullanilabilirLimit(koopSatisFis, (KoopSatisFisHareket) satisFisHareket, false);
//            }
//        }

        if (satisFis.isKooperatifSatisFis()) {
            getSatisFisYonetici().satisFisHareketEkle((KoopSatisFis) satisFis, (KoopSatisFisHareket) satisFisHareket);
        } else {
            getSatisFisYonetici().satisFisHareketEkle((BolgeSatisFis) satisFis, (BolgeSatisFisHareket) satisFisHareket);
        }
    }

    public void desteklemeHesapla(StokHareket satisFisHareket) throws BusinessRuleException {
        BigDecimal desteklemeTutar = BigDecimal.ZERO;
        MerkeziFiyat merkeziFiyat;
        if (satisFisHareket instanceof BolgeSatisFisHareket) {
            TuzelFirmaMusteri muhatap = yukle(TuzelFirmaMusteri.class, ((BolgeSatisFis) satisFisHareket.getStokFis()).getMuhatap().getId());
            Kurum satilanKurum = yukle(Kurum.class, muhatap.getTtkKurum().getId());
            merkeziFiyat = getMerkeziFiyat(satisFisHareket.getStok().getStok(), satilanKurum);
            BigDecimal mot = ((BolgeSatisFisHareket) satisFisHareket).getMusterininOdeyecegiTutar();
            desteklemeTutar = mot.subtract(EkoopUtils.tutarCarp(satisFisHareket.getMiktar(), merkeziFiyat.getTabanFiyat()));
//            mot=mot.subtract(desteklemeTutar);
//            ((BolgeSatisFisHareket) satisFisHareket).setMusterininOdeyecegiTutar(mot);


        }

        if (satisFisHareket instanceof KoopSatisFisHareket) {
            Kurum kurum = yukle(Kurum.class, satisFisHareket.getStokFis().getKurum().getId());
            merkeziFiyat = getMerkeziFiyat(satisFisHareket.getStok().getStok(), kurum);
            desteklemeTutar = ((KoopSatisFisHareket) satisFisHareket).getMusterininOdeyecegiTutar().subtract(EkoopUtils.tutarCarp(satisFisHareket.getMiktar(), merkeziFiyat.getTabanFiyat()));
//            BigDecimal mot = ((KoopSatisFisHareket) satisFisHareket).getMusterininOdeyecegiTutar().subtract(desteklemeTutar);
//            ((KoopSatisFisHareket) satisFisHareket).setMusterininOdeyecegiTutar(mot);
        }

        if (satisFisHareket instanceof AlistanIadeCikisFisHareket) {
            Kurum kurum = yukle(Kurum.class, satisFisHareket.getStokFis().getKurum().getId());
            if (kurum.isMerkez()) {
                //saman destekleme geri iadede acil olarak taban fiyat new BigDecimal("0.5") sonra duzelt
                desteklemeTutar = satisFisHareket.getTutar().subtract(EkoopUtils.tutarCarp(satisFisHareket.getMiktar(), new BigDecimal("0.5")));
            } else {
                merkeziFiyat = getMerkeziFiyat(satisFisHareket.getStok().getStok(), kurum);
                desteklemeTutar = ((AlistanIadeCikisFisHareket) satisFisHareket).getKdvliTutar().subtract(EkoopUtils.tutarCarp(satisFisHareket.getMiktar(), merkeziFiyat.getTabanFiyat()));
            }


        }

        satisFisHareket.setDesteklemeTutari(desteklemeTutar);

    }


    public void merkeziFiyatKontrolMerkezFaturaIcin(BolgeSatisFisHareket satisHareket) throws BusinessRuleException {
        TuzelFirmaMusteri muhatap = yukle(TuzelFirmaMusteri.class, ((BolgeSatisFis) satisHareket.getStokFis()).getMuhatap().getId());
        Kurum satilanKurum = yukle(Kurum.class, muhatap.getTtkKurum().getId());
        MerkeziFiyat merkeziFiyat = getMerkeziFiyat(satisHareket.getStok().getStok(), satilanKurum);
        if (!EkoopUtils.esit(satisHareket.getSatisFiyat().getFiyat(), merkeziFiyat.getFiyat())) {
            throw new BusinessRuleException(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ);
        }
    }

    public MerkeziFiyat getMerkeziFiyat(Stok stok, Kurum kurum) throws BusinessRuleException {
//        Sorgu sorgu = new Sorgu(MerkeziFiyat.class);
//        sorgu.kriterEkle(KriterFactory.esit("aktifMi", true));
//        sorgu.kriterEkle(KriterFactory.esit("stok", stok));
//        sorgu.kriterEkle(KriterFactory.kucuk("baslamaTarihi", getSimdikiTarih()));
//        sorgu.kriterEkle(KriterFactory.or(KriterFactory.esit("kurum", kurum), KriterFactory.esit("kurum", kurum.getUstKurum())));
//        List<MerkeziFiyat> merkeziFiyatlar = sorgula(sorgu);
        MerkeziFiyat sonucMerkeziFiyat = null;
//
//        if (merkeziFiyatlar.size() > 0) {
//            for (MerkeziFiyat merkeziFiyat : merkeziFiyatlar) {
//                if (merkeziFiyat.getKurum().equals(kurum)) {
//                    sonucMerkeziFiyat = merkeziFiyat;
//                } else {
//                    if (merkeziFiyat.getKurum().equals(kurum.getUstKurum()) && merkeziFiyat.getBolgeyeBagliKurumlar()) {
//                        sonucMerkeziFiyat = merkeziFiyat;
//                    }
//                }
//            }
//        }else{
        Sorgu sorgu2 = new Sorgu(MerkeziFiyat.class);
        sorgu2.kriterEkle(KriterFactory.esit("aktifMi", true));
        sorgu2.kriterEkle(KriterFactory.esit("stok", stok));
        sorgu2.kriterEkle(KriterFactory.kucuk("baslamaTarihi", getSimdikiTarih()));
        sorgu2.kriterEkle(KriterFactory.esit("gecerliIl", kurum.getBulunduguIlce().getIl()));
        List<MerkeziFiyat> merkeziFiyatlaril = sorgula(sorgu2);
        if (merkeziFiyatlaril.size() > 0) {
            sonucMerkeziFiyat = merkeziFiyatlaril.get(0);
        }

//        }


        if (sonucMerkeziFiyat != null) {
            return sonucMerkeziFiyat;
        } else {
            throw new BusinessRuleException(StokHataKodu.MERKEZI_FIYAT_BULUNAMADI);
        }

    }


    private MerkeziFiyat getMerkeziFiyatTarihli(Stok stok, Kurum kurum, Tarih tarih, BigDecimal yeniSatisFiyati, BigDecimal satisFiyati) throws BusinessRuleException {
        MerkeziFiyat sonucMerkeziFiyat = null;
        Sorgu sorgu2 = new Sorgu(MerkeziFiyat.class);
        sorgu2.kriterEkle(KriterFactory.esit("stok", stok));
        sorgu2.kriterEkle(KriterFactory.esit("gecerliIl", kurum.getBulunduguIlce().getIl()));
        sorgu2.kriterEkle(KriterFactory.or(KriterFactory.kucuk("baslamaTarihi", new Tarih(tarih.getSaatsizTarih(), 0, 0, 0)), KriterFactory.esit("baslamaTarihi", new Tarih(tarih.getSaatsizTarih(), 0, 0, 0))));
        sorgu2.kriterEkle(KriterFactory.or(KriterFactory.or(KriterFactory.buyuk("bitisTarihi", new Tarih(tarih.getSaatsizTarih(), 0, 0, 0)), KriterFactory.esit("bitisTarihi", new Tarih(tarih.getSaatsizTarih(), 0, 0, 0))), KriterFactory.bos("bitisTarihi")));
        sorgu2.setSiralamaKriteri(new SiralamaKriteri("baslamaTarihi", false));

        List<MerkeziFiyat> merkeziFiyatlari = sorgula(sorgu2);
        Collections.sort(merkeziFiyatlari, new Comparator<MerkeziFiyat>() {
            @Override
            public int compare(MerkeziFiyat mf1, MerkeziFiyat mf2) {
                if (mf1.getBaslamaTarihi().esit(mf2.getBaslamaTarihi()))
                    return mf2.getBitisTarihi().compareTo(mf1.getBitisTarihi());
                else return 0;
            }
        });

        if (merkeziFiyatlari.size() > 0) {
            MerkeziFiyat sonTarihliMerkeziFiyat = null;
            for (int i = 0; i < merkeziFiyatlari.size(); i++) {

                if (i == 0)
                    sonTarihliMerkeziFiyat = merkeziFiyatlari.get(i);
                MerkeziFiyat merkeziFiyat = merkeziFiyatlari.get(i);

                Tarih merkeziFiyatBaslamaTarihi = merkeziFiyatlari.get(i).getBaslamaTarihi();
                Tarih merkeziFiyatBitisTarihi = null;

                if (merkeziFiyatlari.get(i).getBitisTarihi() == null) {
                    Tarih simdikiTarih = getSimdikiTarih();
                    merkeziFiyatBitisTarihi = new Tarih(simdikiTarih.getSaatsizTarih(), simdikiTarih.getSaat(), simdikiTarih.getDakika(), simdikiTarih.getSaniye());
                } else {
                    merkeziFiyatBitisTarihi = merkeziFiyatlari.get(i).getBitisTarihi();
                }


                if (i == 0 || (merkeziFiyatBaslamaTarihi.getSaatsizTarih().beforeOrEqual(sonTarihliMerkeziFiyat.getBaslamaTarihi().getSaatsizTarih())
                        && merkeziFiyatBitisTarihi.getSaatsizTarih().esit(sonTarihliMerkeziFiyat.getBaslamaTarihi().getSaatsizTarih()))) {
                    boolean merkeziFiyataUygunMu = true;
                    if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                        if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, merkeziFiyat.getFiyat()) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                            merkeziFiyataUygunMu = false;
                        }

                        if (EkoopUtils.isKucuk(yeniSatisFiyati, merkeziFiyat.getTabanFiyat()) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                            merkeziFiyataUygunMu = false;
                        }

                    } else {
                        if (!EkoopUtils.isKucukEsit(satisFiyati, merkeziFiyat.getFiyat()) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                            merkeziFiyataUygunMu = false;
                        }

                        if (EkoopUtils.isKucuk(satisFiyati, merkeziFiyat.getTabanFiyat()) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                            merkeziFiyataUygunMu = false;
                        }
                    }

                    if (merkeziFiyataUygunMu) {
                        sonucMerkeziFiyat = merkeziFiyat;
                        break;
                    }
                }
            }

            if (sonucMerkeziFiyat == null) {
                sonucMerkeziFiyat = sonTarihliMerkeziFiyat;
            }
        }

        if (sonucMerkeziFiyat != null) {
            return sonucMerkeziFiyat;
        } else {
            throw new BusinessRuleException(StokHataKodu.MERKEZI_FIYAT_BULUNAMADI);
        }
    }


    public List<MobilKoop> enYakinKooperatif(String enlem, String boylam) {

        Sorgu sorgu = new Sorgu(Kooperatif.class);
        sorgu.kriterEkle(KriterFactory.esit("tip", KurumTip.KOOPERATIF));
        sorgu.kriterEkle(KriterFactory.esit("durum", KurumDurum.FAAL));
        List<Kooperatif> kooperatifler = sorgula(sorgu);
        List<MobilKoop> mobilKoops = new ArrayList<MobilKoop>();
        for (Kooperatif kooperatif : kooperatifler) {
            MobilKoop mobilKoop = new MobilKoop((long) dunyaNoktadanUzaklik(Float.parseFloat(enlem), Float.parseFloat(boylam), Float.parseFloat(kooperatif.getEnlem() + ""), Float.parseFloat(kooperatif.getBoylam() + "")), kooperatif.getKurumNo(), kooperatif.getAd(), kooperatif.getHaberlesmeAdresi(), kooperatif.getTelefon());
            mobilKoops.add(mobilKoop);
        }

        Collections.sort(mobilKoops);
        return mobilKoops.subList(0, 5);
    }

    private float dunyaNoktadanUzaklik(float bulundugunEnlem, float bulundugunBoylam, float karsilastirlacakEnlem, float karsilastirlacakBoylam) {
        double yeryuzuYaricap = 6371000; //metre
        double denlemBirim = Math.toRadians(karsilastirlacakEnlem - bulundugunEnlem);
        double dboylamBirim = Math.toRadians(karsilastirlacakBoylam - bulundugunBoylam);
        double a = Math.sin(denlemBirim / 2) * Math.sin(denlemBirim / 2) +
                Math.cos(Math.toRadians(bulundugunEnlem)) * Math.cos(Math.toRadians(karsilastirlacakEnlem)) *
                        Math.sin(dboylamBirim / 2) * Math.sin(dboylamBirim / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float mesafe = (float) (yeryuzuYaricap * c);
//donus metre cinsinden donuyorum
        return mesafe;
    }


    private void validateTakipOzellik(SatisFis satisFis, SatisFisHareket hareket) throws BusinessRuleException {
        if (hareket.getStok().isPartiTakipOzellikli() || hareket.getStok().isSarjTakipOzellikli()) {
            if (hareket.getTakipOzelligi().getSonKullanmaTarihi().beforeOrEqual(satisFis.getFisTarihi())) {
                throw new BusinessRuleException(StokHataKodu.SON_KULLANMA_TARIHI_GECMIS_URUNU_SATAMAZSINIZ);
            }
        }
    }

    private void validateOrtakVeresiyeKullanilabilirLimit(KoopSatisFis koopSatisFis, KoopSatisFisHareket koopSatisFisHareket, boolean hareketEklemeMi) throws BusinessRuleException {
        BigDecimal ortaginVeresiyeLimiti = ortakKrediModulServis.getVeresiyeliSatisKrediLimiti(koopSatisFis.getMuhatap().getOrtak());
        BigDecimal ortaginKullanilanVeresiyeLimiti = ortakKrediModulServis.getKullanilanVeresiyeliSatisLimiti(koopSatisFis.getMuhatap().getOrtak());
        BigDecimal hareketTutari = koopSatisFisHareket.getMusterininOdeyecegiTutar();
        BigDecimal koopSatisFisTopMOT = BigDecimal.ZERO;
        List<KoopSatisFisHareket> fisHareketleri = koopSatisFis.getHareketler();
        for (KoopSatisFisHareket hareket : fisHareketleri) {
            if (hareketEklemeMi) {
                koopSatisFisTopMOT = koopSatisFisTopMOT.add(koopSatisFisHareket.getMusterininOdeyecegiTutar());
            } else {
                if (hareket.getId() != koopSatisFisHareket.getId()) {
                    koopSatisFisTopMOT = koopSatisFisTopMOT.add(koopSatisFisHareket.getMusterininOdeyecegiTutar());
                }
            }

        }
        koopSatisFisTopMOT = koopSatisFisTopMOT.add(hareketTutari);
        if (ortaginVeresiyeLimiti.compareTo(ortaginKullanilanVeresiyeLimiti.add(koopSatisFisTopMOT)) < 0) {
            throw new BusinessRuleException("Bu işlem için veresiye Satış Limiti Yetersiz. Veresiye Limiti : " + ortaginVeresiyeLimiti + " TL. Kullanılan Veresiye Limiti : " + ortaginKullanilanVeresiyeLimiti + " TL. Kalan Limit : "
                    + EkoopUtils.cikar(ortaginVeresiyeLimiti, ortaginKullanilanVeresiyeLimiti) + " TL. Stok Fişindeki Toplam Tutar :" + koopSatisFisTopMOT);
        }

    }

    public List<SatisFis> getSatisFisler(Sorgu sorgu, Kurum aktifKurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", aktifKurum));
        return dao.sorgula(sorgu);
    }

    public Integer kurumMaxVeresiyeSenetNo(Kullanici kullanici){

        Sorgu maxVeresiyeSenetSorgu = new Sorgu(KoopSatisFis.class);
        maxVeresiyeSenetSorgu.kriterEkle(KriterFactory.esit("kurum", kullanici.getAktifKurum()));
        maxVeresiyeSenetSorgu.kriterEkle(KriterFactory.esit("odemeSekli", KoopSatisOdemeSekli.VERESIYE));
        maxVeresiyeSenetSorgu.kriterEkle(KriterFactory.dolu("vadeliSenetNo"));
        maxVeresiyeSenetSorgu.kriterEkle(KriterFactory.buyuk("yil", 2018));
        maxVeresiyeSenetSorgu.setSiralamaKriteri(new SiralamaKriteri("vadeliSenetNo",false));
        List<KoopSatisFis> veresiyeSenetList = sorgula( maxVeresiyeSenetSorgu );
        if( !veresiyeSenetList.isEmpty())
            return  veresiyeSenetList.get(0).getVadeliSenetNo();
        else
            return 0;



    }

    public MahsupFis satisFisOnayla(SatisFis satisFis, Kullanici kullanici) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(satisFis.getKurum(), "StokFisOnayla", String.valueOf(satisFis.getId()), true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }
//ismail veresiye satıs işlemi yapınca ac alt islemi
//        if (satisFis instanceof KoopSatisFis) {
//            KoopSatisFis koopSatisFis = (KoopSatisFis) satisFis;
//            if (koopSatisFis.getOdemeSekli().equals(KoopSatisOdemeSekli.VERESIYE) && koopSatisFis.getMuhatapTip().equals(MuhatapTip.ORTAK_ICI)) {
//                return ortakIciVeresiyeSatisSenetleOnayla(koopSatisFis, kullanici);
//            }
//        }


        if(  satisFis instanceof  KoopSatisFis  && satisFis.isVadeliMi() ){
            VeresiyeIslem odendiIslem = new VeresiyeIslem();

            odendiIslem.setAlacak( BigDecimal.ZERO );
            odendiIslem.setBorc( BigDecimal.ZERO );
            odendiIslem.setKurum( (Kooperatif) kullanici.getAktifKurum() );
            odendiIslem.setIslemTarihi( getSimdikiTarih() );
            odendiIslem.setValorTarihi( getSimdikiTarih().dakikaEkle(1));
            odendiIslem.setStokFis( (KoopSatisFis) satisFis );
            odendiIslem.setVeresiyeIslemTipi( VeresiyeIslemTipi.STOK_FATURA_DUZENLEME);
            odendiIslem.setTahsilatTipi( TahsilatTipi.TAHSIL );
            odendiIslem.setBorc( ((KoopSatisFis) satisFis).getSumMOTTutar());

            kaydet( odendiIslem );

            ((KoopSatisFis) satisFis).setVadeliSenetNo( kurumMaxVeresiyeSenetNo(kullanici)+1 );
        }



        if (satisFis.getMuhatap().isGercek()) {
//            try {
//                oluOrtakKontrol(satisFis.getMuhatap().getOrtak().getTcKimlikNo());
//            } catch (NviServiceException e) {
//                e.printStackTrace();
//            }
        }

// todo:şeyma duzerltince ac 13/02/2015
        if (satisFis.getMuhatap().getTip().equals(GercekTuzel.TUZEL)) {
            //lazy aldigimiz için yeniden yukledik
            TuzelFirmaMusteri firma = yukle(TuzelFirmaMusteri.class, satisFis.getMuhatap().getId());
            if (firma.getTtkKurum() != null) {
                if (satisFis.getKurum().isBolge() && satisFis.getMuhatapTip().equals(MuhatapTip.TESKILAT_ICI)) {
                    Kurum alanKurum = yukle(Kurum.class, firma.getTtkKurum().getId());
                    krediYonetimiBilgiGir(satisFis, kullanici, alanKurum);
                }
            }
        }

        return getSatisFisYonetici().satisFisOnayla(satisFis, kullanici);
    }

    public MahsupFis satisFisOnaylaGencCiftci(SatisFis satisFis, Kullanici kullanici) throws BusinessRuleException {

        //TODO "StokFisOnayla" bunu ayrıca genc ciftci ici ntanımlamak gerekecek mi???
        if (!canEnterStokCriticalSection(satisFis.getKurum(), "StokFisOnayla", String.valueOf(satisFis.getId()), true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }

        return getSatisFisYonetici().satisFisOnaylaGencCiftci(satisFis, kullanici);
    }

    private MahsupFis ortakIciVeresiyeSatisSenetleOnayla(KoopSatisFis koopSatisFis, Kullanici kullanici) throws BusinessRuleException {
//        MahsupFis mahsupFis = new MahsupFis(getSimdikiTarih(), koopSatisFis.getKurum(), FisKaynak.ORTAK_KREDI, "VERESİYE STIŞ MUHASEBESİ");
//        muhasebeModulServis.geciciMahsupFisiAc(mahsupFis, kullanici);
//        veresiyeSatisStokMuhasebeHareketleriOlustur(koopSatisFis, mahsupFis);
//        koopSatisFis.setMuhasebeFisi(mahsupFis);
        ortakKrediModulServis.ortakIciVeresiyeSatisSenetleOnayla(koopSatisFis, kullanici);
        return (MahsupFis) koopSatisFis.getMuhasebeFisi();
//        return mahsupFis;
    }

    private void veresiyeSatisStokMuhasebeHareketleriOlustur(KoopSatisFis koopSatisFis, MahsupFis mahsupFis) throws BusinessRuleException {
        getSatisFisYonetici().veresiyeSatisStokMuhasebeHareketleriOlustur(koopSatisFis, mahsupFis);
    }

    private void oluOrtakKontrol(String tcKimlikNo) throws NviServiceException, BusinessRuleException {
        KisiModel mernisdenGelenKisi = null;//ortakTakipModulServis.mernisIleTcKimlikSorgula(tcKimlikNo, ModulAdi.STOK, getSimdikiTarih());
        if (mernisdenGelenKisi.getAd() != null) {  // TC NOsu Hatalı Olanlar
            if (mernisdenGelenKisi.getOlumTarih().getGun() != null) {
                throw new BusinessRuleException(OrtakTakipHataKodu.TC_KIMLIK_NUMARASINA_GORE_KISI_OLU_GORUNUYOR_KAYIT_YAPILAMAZ);
            }
        } else {
            throw new BusinessRuleException(OrtakTakipHataKodu.TC_KIMLIK_NUMARASI_SORGULAMADA_BULUNAMADI);
        }

    }

    public void krediYonetimiBilgiGir(SatisFis satisFis, Kullanici kullanici, Kurum alanKurum) throws BusinessRuleException {
//        TuzelFirmaMusteri muhatap = yukle(TuzelFirmaMusteri.class, satisFis.getMuhatap().getId());
//        Kurum muhatapKurum = yukle(Kurum.class,muhatap.getTtkKurum().getId());
        krediYonetimiModulServis.stoktanFonaBorclandirmaVeIptalIslemleriAktarimi((BolgeSatisFis) satisFis, alanKurum, KrediYonetimiStokIslemTipi.BORCLANDIRMA, kullanici);

    }

    public MahsupFis bolgeSatisFisOnayla(BolgeSatisFis satisFis, Kullanici kullanici) throws BusinessRuleException {
        return this.satisFisOnayla(satisFis, kullanici);
    }

    public StokIptalFis satisFiyatArtirimFisIptal(SatisFiyatArttirimFis satisFiyatArttirimFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().satisFiyatArtirimFisIptal(satisFiyatArttirimFis, kullanici, kesilenFisler);
    }

    public void satisFiyatArttirimFisiKes(SatisFiyatArttirimFis satisFiyatArttirimFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(satisFiyatArttirimFis, kullanici);
    }

    public void satisFiyatArttirimFisOnayla(SatisFiyatArttirimFis fiyatArttirimFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().satifFiyatArttirimFisOnayla(fiyatArttirimFis, kullanici);
    }

    public void fireCikisFisiKes(FireCikisFis fireCikisFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(fireCikisFis, kullanici);
    }

    public void fireFisCikisHareketEkle(FireCikisFis fireCikisFis, FireCikisHareket hareket) throws BusinessRuleException {
        getStokFisYonetici().fireFisCikisHareketEkle(fireCikisFis, hareket);
    }

    public List getFireCikisFisleri(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void fireCikisFisiOnayla(FireCikisFis fireCikisFis) throws BusinessRuleException {
        getStokFisYonetici().fireCikisFisiOnayla(fireCikisFis);
    }

    public StokIptalFis fireCikisFisIptal(FireCikisFis fireCikisFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().fireCikisFisIptal(fireCikisFis, kullanici, kesilenFisler);
    }

    public void bolgeSatisFiyatkaydet(BolgeSatisFiyat stokSatisFiyat) throws BusinessRuleException {
        if (stokSatisFiyat.getGecerliTarih().before(getSimdikiTarih())) {
            throw new BusinessRuleException(StokHataKodu.GERIYE_DONUK_FIYAT_GIRISI_YAPAMAZSINIZ);
        }

        if (stokSatisFiyat.isVadeli() && stokSatisFiyat.getVadeGunSayisi() == null) {
            throw new BusinessRuleException(StokHataKodu.VADE_GUN_SAYISI_BOS_GECILEMEZ);

        }

        if (stokSatisFiyat.isVadeli() && stokSatisFiyat.getVadeGunSayisi() > 360) {
            throw new BusinessRuleException(StokHataKodu.VADE_GUN_SAYISI_360_GUNDEN_FAZLA_OLAMAZ);

        }

        kaydet(stokSatisFiyat);
    }

    public void stokIskontoKaydet(StokIskonto stokIskonto, Kurum kurum) throws BusinessRuleException {
        stokIskonto.setKurum(kurum);

        if (stokIskonto.getGecerliTarih().before(getSimdikiTarih())) {
            throw new BusinessRuleException(StokHataKodu.GERIYE_DONUK_ISKONTO_GIRISI_YAPAMAZSINIZ);
        }

        if (stokIskonto.getIskontoTipi().equals(StokIskontoTip.GENEL)) {
            Sorgu sorgu = new Sorgu(StokIskonto.class);

            sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
            sorgu.kriterEkle(KriterFactory.esit("iskontoTipi", StokIskontoTip.GENEL));
            sorgu.kriterEkle(KriterFactory.esit("gecerliTarih", stokIskonto.getGecerliTarih()));

            List<StokIskonto> stokIskontolar = dao.sorgula(sorgu);

            if (stokIskontolar.size() > 0) {
                throw new BusinessRuleException(StokHataKodu.AYNI_BILGILERLE_GENEL_ISKONTO_BILGILERI_GIRILMISTIR);
            }
        } else if (stokIskonto.getIskontoTipi().equals(StokIskontoTip.STOK)) {
            Sorgu sorgu = new Sorgu(StokIskonto.class);

            sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
            sorgu.kriterEkle(KriterFactory.esit("iskontoTipi", StokIskontoTip.STOK));
            sorgu.kriterEkle(KriterFactory.esit("gecerliTarih", stokIskonto.getGecerliTarih()));
            sorgu.kriterEkle(KriterFactory.esit("kurumStok", stokIskonto.getKurumStok()));

            List<StokIskonto> stokIskontolar = dao.sorgula(sorgu);

            if (stokIskontolar.size() > 0) {
                throw new BusinessRuleException(StokHataKodu.AYNI_BILGILERLE_BU_STOK_ICIN_ISKONTO_BILGILERI_GIRILMISTIR);
            }
        } else if (stokIskonto.getIskontoTipi().equals(StokIskontoTip.STOK_GRUBU)) {
            Sorgu sorgu = new Sorgu(StokIskonto.class);

            sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
            sorgu.kriterEkle(KriterFactory.esit("iskontoTipi", StokIskontoTip.STOK_GRUBU));
            sorgu.kriterEkle(KriterFactory.esit("gecerliTarih", stokIskonto.getGecerliTarih()));
            sorgu.kriterEkle(KriterFactory.esit("stokGrubu", stokIskonto.getStokGrubu()));

            List<StokIskonto> stokIskontolar = dao.sorgula(sorgu);

            if (stokIskontolar.size() > 0) {
                throw new BusinessRuleException(StokHataKodu.AYNI_BILGILERLE_BU_STOK_GRUBU_ICIN_ISKONTO_BILGILERI_GIRILMISTIR);
            }
        }

        kaydet(stokIskonto);
    }

    public List<StokIskonto> getStokIskontolar(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public List<KurumStok> getKurumStoklari(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void stokIciAktarimFisiKes(StokIciAktarimFis fis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokIciAktarimFisiKes(fis, kullanici);
    }

    public List<StokTakipOzellikDurum> getTakipOzelligineGoreStokDurum(KurumStok kurumStok) {
        return sorgula(new Sorgu(StokTakipOzellikDurum.class, null, KriterFactory.esit("kurumStok", kurumStok)));
    }

    public void stokIciAktarimGunceller(StokIciAktarimFis fis) throws BusinessRuleException {
        getStokFisYonetici().stokIciAktarimFisiGuncelle(fis);
    }

    public void konsinyeGirisFisiKes(KonsinyeGirisFis konsinyeGirisFis, Kullanici kullanici) throws BusinessRuleException {
        getKonsinyeFisYonetici().stokFisKes(konsinyeGirisFis, kullanici);
    }

    public void konsinyeGirisFisHareketEkle(KonsinyeGirisFis konsinyeGirisFis, KonsinyeGirisFisHareket konsinyeGirisFisHareket) throws BusinessRuleException {
        getKonsinyeFisYonetici().konsinyeGirisFisHareketEkle(konsinyeGirisFis, konsinyeGirisFisHareket);
    }

    public List<KonsinyeGirisFis> getKonsinyeGirisFisler(Kurum aktifKurum) {
        return dao.sorgula(new Sorgu(KonsinyeGirisFis.class, null, KriterFactory.esit("kurum", aktifKurum)));
    }

    public void konsinyeGirisFisOnayla(KonsinyeGirisFis konsinyeGirisFis) throws BusinessRuleException {
        getKonsinyeFisYonetici().konsinyeGirisFisOnayla(konsinyeGirisFis);
    }

    public List<KoopSatisFiyat> getKoopSatisFiyatlari(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurumStok.kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void koopSatisFiyatkaydet(KoopSatisFiyat koopStokSatisFiyat) throws BusinessRuleException {
        koopStokSatisFiyat.setFiyatTipi(KoopSatisFiyatTip.PESIN);
        if (koopStokSatisFiyat.getGecerliTarih().before(getSimdikiTarih())) {
            throw new BusinessRuleException(StokHataKodu.GERIYE_DONUK_FIYAT_GIRISI_YAPAMAZSINIZ);
        }
        Stok stok = koopStokSatisFiyat.getKurumStok().getStok();
        if (stok.getMerkeziFiyatMi()) {


            MerkeziFiyat merkeziFiyat = getMerkeziFiyat(stok, koopStokSatisFiyat.getKurumStok().getKurum());

            if ((koopStokSatisFiyat.getMuhatapTipi().equals(MuhatapTip.ORTAK_ICI) ||
                    koopStokSatisFiyat.getMuhatapTipi().equals(MuhatapTip.TESKILAT_ICI) || koopStokSatisFiyat.getMuhatapTipi().equals(MuhatapTip.ORTAK_DISI))) {
                if (stok.getTavanFiyatUygulanacak()) {
                    if (!EkoopUtils.isKucukEsit(koopStokSatisFiyat.getFiyat(), merkeziFiyat.getFiyat())) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, merkeziFiyat.getFiyat().toString());
                    }
                }


            }

            if ((koopStokSatisFiyat.getMuhatapTipi().equals(MuhatapTip.ORTAK_ICI) || koopStokSatisFiyat.getMuhatapTipi().equals(MuhatapTip.ORTAK_DISI))) {
                if (stok.getTabanFiyatUygulanacak()) {
                    if (EkoopUtils.isKucuk(koopStokSatisFiyat.getFiyat(), merkeziFiyat.getTabanFiyat())) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, merkeziFiyat.getTabanFiyat().toString());
                    }

                }

            }

        }


        if (stok.isYemStokKartMi() && !koopStokSatisFiyat.getMuhatapTipi().equals(MuhatapTip.TESKILAT_ICI)) {
            BigDecimal olmasiGerekenFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), koopStokSatisFiyat.getKurumStok());

            BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), koopStokSatisFiyat.getKurumStok());


            if (EkoopUtils.isKucuk(koopStokSatisFiyat.getFiyat(), olmasiGerekenMinTabanFiyat)) {
                throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString(), olmasiGerekenFiyat.toString());
//                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString());
            }
            if (EkoopUtils.isBuyuk(koopStokSatisFiyat.getFiyat(), olmasiGerekenFiyat)) {
                throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, olmasiGerekenFiyat.toString());
            }
        }

        kaydet(koopStokSatisFiyat);
    }

    public BigDecimal yemTavanFiyat(Parametre parametre, KurumStok kurumStok) {
        BigDecimal paramtreOran = parametre.getBigDecimalDeger();
        BigDecimal ortlamaMaliyet = getOrtalamaMaliyet(kurumStok);
        BigDecimal olmasiGerekenFiyat = ortlamaMaliyet.multiply(paramtreOran);
        olmasiGerekenFiyat = EkoopUtils.yuvarla(olmasiGerekenFiyat.add(ortlamaMaliyet), 2);
        return olmasiGerekenFiyat;
    }

    public BigDecimal yemTavanFiyatKrediGecmisTarih(Parametre parametre, KurumStok kurumStok, Tarih tarih) {
        BigDecimal paramtreOran = parametre.getBigDecimalDeger();
        BigDecimal ortlamaMaliyet = getOrtalamaMaliyetTarihli(kurumStok, tarih);
        BigDecimal olmasiGerekenFiyat = ortlamaMaliyet.multiply(paramtreOran);
        olmasiGerekenFiyat = EkoopUtils.yuvarla(olmasiGerekenFiyat.add(ortlamaMaliyet), 2);
        return olmasiGerekenFiyat;
    }

    public KoopSatisFiyat getSatisFiyatKoop(KurumStok kurumStok, Tarih tarih, MuhatapTip muhatapTip) throws BusinessRuleException {
        Tarih fiyatTarih;

        if (tarih.equals(getSimdikiTarih())) {
            fiyatTarih = getSimdikiTarih();
        } else {
            fiyatTarih = tarih;
        }

        KoopSatisFiyat satisFiyat = dao.getSatisFiyatKoop(kurumStok, muhatapTip, fiyatTarih, KoopSatisFiyatTip.PESIN);

        if (satisFiyat == null) {
            throw BusinessRuleException.olustur(StokHataKodu.KOOPERATIF_SATIS_FIYATI_BULUNAMADI, kurumStok.getStok().getStokKodu(), muhatapTip.name(), tarih.toString());
        }

        return satisFiyat;
    }

    public List<KoopSatisFiyat> getSatisFiyatKooplariForKredi(KurumStok kurumStok, Tarih tarih, MuhatapTip muhatapTip) throws BusinessRuleException {
        Tarih fiyatTarih;

        if (tarih.equals(getSimdikiTarih())) {
            fiyatTarih = getSimdikiTarih();
        } else {
            fiyatTarih = tarih;
        }

        Sorgu sabitFiyatSorgu = new Sorgu(SabitFiyatStok.class);
        sabitFiyatSorgu.kriterEkle(KriterFactory.esit("stok", kurumStok.getStok()));
        sabitFiyatSorgu.kriterEkle(KriterFactory.kucuk("baslangicTarihi", tarih.gunEkle(1)));
        SiralamaKriteri siralamaKriteri = new SiralamaKriteri("baslangicTarihi", false);
        sabitFiyatSorgu.setSiralamaKriteri(siralamaKriteri);
        List<SabitFiyatStok> sabitFiyatStokList = sorgula(sabitFiyatSorgu);


        List<KoopSatisFiyat> satisFiyat = dao.getSatisFiyatKooplariForKredi(kurumStok, muhatapTip, fiyatTarih, KoopSatisFiyatTip.PESIN);

        if (satisFiyat == null) {
            throw BusinessRuleException.olustur(StokHataKodu.KOOPERATIF_SATIS_FIYATI_BULUNAMADI, kurumStok.getStok().getStokKodu(), muhatapTip.name(), tarih.toString());
        }

        if (!sabitFiyatStokList.isEmpty()) {
            List<KoopSatisFiyat> satisFiyatReturnValue = new ArrayList<KoopSatisFiyat>();
            for (KoopSatisFiyat koopSatisFiyat : satisFiyat) {
                if (sabitFiyatStokList.get(0).getBaslangicTarihi().before(koopSatisFiyat.getGecerliTarih()) &&
                        EkoopUtils.isEsit(sabitFiyatStokList.get(0).getFiyat(), koopSatisFiyat.getFiyat())) {
                    satisFiyatReturnValue.add(koopSatisFiyat);
                }
            }

            if (satisFiyatReturnValue.size() == 0) {
                throw new BusinessRuleException("Girdiğiniz stok merkez birliği tarafından sabit fiyatlı olarak tanımlanmıştır. Bu stoğun kooperatif satış fiyatını girmeniz gerekmektedir!");
            } else {
                return satisFiyatReturnValue;
            }

        }
        return satisFiyat;
    }

    public StokIskonto getStokIskonto(Kurum bolge, KurumStok kurumStok, Tarih tarih, KoopSatisFiyatTip koopSatisFiyatTip) throws BusinessRuleException {
        StokIskonto stokIskonto = getStokIskontoByKurumStok(bolge, kurumStok, tarih);

        if (stokIskonto != null && satisFiyatTipineGoreStokIskontoDoluMu(stokIskonto, koopSatisFiyatTip)) {
            return stokIskonto;
        }

        stokIskonto = dao.getStokIskontoByStokGrubu(bolge, kurumStok.getStok().getGrup(), tarih);
        if (stokIskonto != null && satisFiyatTipineGoreStokIskontoDoluMu(stokIskonto, koopSatisFiyatTip)) {
            return stokIskonto;
        }

        stokIskonto = dao.getStokIskontoByGenel(bolge, tarih);
        if (stokIskonto != null && satisFiyatTipineGoreStokIskontoDoluMu(stokIskonto, koopSatisFiyatTip)) {
            return stokIskonto;
        }

        throw new BusinessRuleException(StokHataKodu.BOLGE_BIRLIGINIZ_STOK_ISKONTO_BILGILERINI_GIRMEMISTIR);
    }

    public BolgeSatisFiyat getSatisFiyatBolge(KurumStok kurumStok, Tarih tarih, BolgeSatisFiyatTip bolgeSatisFiyatTip, Il musterininIli, Integer vadeGunSayisi, Ilce ilce) throws BusinessRuleException {
        Tarih fiyatTarih;

        if (tarih.equals(getSimdikiTarih())) {
            fiyatTarih = getSimdikiTarih();
        } else {
            fiyatTarih = tarih;
        }

        BolgeSatisFiyat satisFiyat = dao.getSatisFiyatBolge(kurumStok, fiyatTarih, bolgeSatisFiyatTip, musterininIli, vadeGunSayisi, ilce);
        if (satisFiyat == null) {
            // müşterinin iline göre kayıt bulunamazsa genel satış fiyatı bulunur
            satisFiyat = dao.getSatisFiyatBolge(kurumStok, fiyatTarih, bolgeSatisFiyatTip, null, vadeGunSayisi, ilce);

            if (satisFiyat == null) {
                throw BusinessRuleException.olustur(StokHataKodu.BOLGE_SATIS_FIYATI_BULUNAMADI, kurumStok.getStok().getStokKodu(), tarih.toString(), bolgeSatisFiyatTip.name());
            }
        }

        return satisFiyat;
    }

    public List<BolgeSatisFiyat> getBolgeSatisFiyatlari(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurumStok.kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public StokIptalFis konsinyeGirisFisIptal(KonsinyeGirisFis konsinyeGirisFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getKonsinyeFisYonetici().konsinyeGirisFisIptal(konsinyeGirisFis, kullanici, kesilenFisler);
    }

    public void konsinyeIadeFisiKes(KonsinyeIadeFis konsinyeIadeFis, Kullanici aktifKullanici) throws BusinessRuleException {
        getKonsinyeFisYonetici().stokFisKes(konsinyeIadeFis, aktifKullanici);
    }

    public void konsinyeIadeFisHareketEkle(KonsinyeIadeFis konsinyeIadeFis, KonsinyeIadeFisHareket konsinyeIadeFisHareket) throws BusinessRuleException {
        getKonsinyeFisYonetici().konsinyeCikisFisHareketEkle(konsinyeIadeFis, konsinyeIadeFisHareket);
    }

    public List<KonsinyeIadeFis> getKonsinyeIadeFisler(Kurum aktifKurum) {
        return dao.sorgula(new Sorgu(KonsinyeIadeFis.class, null, KriterFactory.esit("kurum", aktifKurum)));
    }

    public MahsupFis konsinyeCikisFisOnayla(KonsinyeIadeFis konsinyeIadeFis) throws BusinessRuleException {
        return getKonsinyeFisYonetici().konsinyeCikisFisOnayla(konsinyeIadeFis);
    }

    public void uretimFisiKes(UretimFis uretimFis, Kullanici aktifKullanici) throws BusinessRuleException {
//        if (uretimFis.getKulakKupeNo() != null) {
//
//            if (EkoopUtils.isBuyuk(uretimFis.getMiktar(), BigDecimal.ONE)) {
//                throw new BusinessRuleException(StokHataKodu.MIKTAR_HATALI_KONTROL_EDINIZ);
//            }
//
//            if (!uretimFis.getKulakKupeNo().substring(0, 2).equals("TR")) {
//                throw new BusinessRuleException(StokHataKodu.KULAK_KUPE_NUMARASI_KONTROL_EDINIZ);
//            }
//
//            if (uretimFis.getKulakKupeNo().length() < 14) {
//                throw new BusinessRuleException(StokHataKodu.KULAK_KUPE_NUMARASI_KONTROL_EDINIZ);
//            }
//
//            boolean girilmisMi = validateUretimFisiKulakKupeOncedenGirilmisMi(uretimFis);
//            if (girilmisMi) {
//                throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
//            }
//        }
        getUretimFisYoneticisi().stokFisKes(uretimFis, aktifKullanici);
    }

    public void uretimFisineYanUrunEkle(UretimFis uretimFis, UretimYanUrun uretimYanUrun) throws BusinessRuleException {
        getUretimFisYoneticisi().uretimFisineYanUrunEkle(uretimFis, uretimYanUrun);
    }

    public void uretimFisineKullanilanUrunEkle(UretimFis uretimFis, UretimKullanilanUrun uretimKullanilanUrun) throws BusinessRuleException {
        getUretimFisYoneticisi().uretimFisineKullanilanUrunEkle(uretimFis, uretimKullanilanUrun);
    }

    public void uretimFisineHizmetEkle(UretimFis uretimFis, UretimHizmet uretimHizmet) throws BusinessRuleException {
        getUretimFisYoneticisi().uretimFisineHizmetEkle(uretimFis, uretimHizmet);
    }

    public void uretimFisiOnayla(UretimFis uretimFis, EkHizmetBuro ekHizmetBuro) throws BusinessRuleException {
        getUretimFisYoneticisi().uretimFisiOnayla(uretimFis, ekHizmetBuro);
    }

    public List<KurumStok> getTicariKurumStoklar(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.benzer("stok.stokMuhasebeHesabi.hesapNo", "153"));
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public List<KurumStok> getHamMaddeStoklar(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.benzer("stok.stokMuhasebeHesabi.hesapNo", "150"));
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void yanUrunGuncelle(UretimYanUrun uretimYanUrun) throws BusinessRuleException {
        getUretimFisYoneticisi().yanUrunGuncelle(uretimYanUrun);
    }

    public void uretimFisindenYanUrunSil(UretimFis uretimFisi, UretimYanUrun uretimYanUrun) throws BusinessRuleException {
        getUretimFisYoneticisi().uretimFisindenYanUrunSil(uretimFisi, uretimYanUrun);
    }

    public void uretimFisindenKullanilanUrunSil(UretimFis uretimFisi, UretimKullanilanUrun uretimKullanilanUrun) throws BusinessRuleException {
        getUretimFisYoneticisi().uretimFisindenKullanilanUrunSil(uretimFisi, uretimKullanilanUrun);
    }

    public void kullanilanUrunGuncelle(UretimKullanilanUrun uretimKullanilanUrun) throws BusinessRuleException {
        getUretimFisYoneticisi().kullanilanUrunGuncelle(uretimKullanilanUrun);
    }

    public BigDecimal kullanilanUrunTutariniHesapla(UretimKullanilanUrun uretimKullanilanUrun) {
        return getUretimFisYoneticisi().kullanilanUrunTutariniHesapla(uretimKullanilanUrun);
    }

    public SatisIptalFis satisFisIptal(SatisFis satisFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(satisFis.getKurum(), "StokFisIptal", satisFis.getFisNo(), true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }
        // todo:şeyma duzerltince ac 13/02/2015
        if (satisFis.getMuhatap().getTip().equals(GercekTuzel.TUZEL)) {
            //lazy aldigimiz için yeniden yukledik
            TuzelFirmaMusteri firma = yukle(TuzelFirmaMusteri.class, satisFis.getMuhatap().getId());
            if (firma.getTtkKurum() != null) {
                if (satisFis.getKurum().isBolge() && satisFis.getMuhatapTip().equals(MuhatapTip.TESKILAT_ICI)) {
                    Kurum alanKurum = yukle(Kurum.class, firma.getTtkKurum().getId());
                    krediYonetimiBilgiIptalGir(satisFis, kullanici, alanKurum);
                }
            }
        }

        if( satisFis instanceof KoopSatisFis &&  satisFis.isVadeliMi() ){
            Sorgu veresiyeIslemSorgu = new Sorgu(VeresiyeIslem.class);
            veresiyeIslemSorgu.kriterEkle(KriterFactory.esit("stokFis", satisFis));
            veresiyeIslemSorgu.kriterEkle(KriterFactory.esit("veresiyeIslemTipi", VeresiyeIslemTipi.TAHSILAT));
            veresiyeIslemSorgu.kriterEkle(KriterFactory.bos( "iptalDurum"));

            List<VeresiyeIslem> iptalEdilmemisTahsilatlar = sorgula( veresiyeIslemSorgu );

            Parametre parametre = commonServis.getParametre(satisFis.getKurum(),"STK_VERESIYE_MAX_IPTAL_GUN_SAYISI", true);
            if( !iptalEdilmemisTahsilatlar.isEmpty()){

                throw new BusinessRuleException("Satış Fişine Ait Tahsilat Bulunmaktadır. Öncelikle Bu İşlemlerin İptali Gerekmektedir.");
            }else if( iptalEdilmemisTahsilatlar.isEmpty() && getSimdikiTarih().gunCikar(parametre.getIntDeger() ).after( satisFis.getFisTarihi()) ){


                throw new BusinessRuleException(parametre.getIntDeger()+" "+"Günü Geçen Veresiye Satışların İptalini Gerçekleştiremezsiniz.");

            }
        }

        SatisIptalFis satisIptalFis = getSatisFisYonetici().satisFisIptal(satisFis, kullanici, kesilenFisler);

//        Sorgu iptalHareketList = new Sorgu(StokIptalFisHareket.class);
//        iptalHareketList.kriterEkle(KriterFactory.esit("stokFis",satisIptalFis.getIptalEdilenFis()));
//
//        sorgula( iptalHareketList );
//        getSatisFisYonetici().satisIptalKarekodIptali( satisFis, satisIptalFis.getIptalEdenFis());


        return satisIptalFis;
    }

    public void krediYonetimiBilgiIptalGir(SatisFis satisFis, Kullanici kullanici, Kurum alanKurum) throws BusinessRuleException {
//        TuzelFirmaMusteri muhatap = yukle(TuzelFirmaMusteri.class, satisFis.getMuhatap().getId());
//        Kurum muhatapKurum = yukle(Kurum.class,muhatap.getTtkKurum().getId());
        krediYonetimiModulServis.stoktanFonaBorclandirmaVeIptalIslemleriAktarimi((BolgeSatisFis) satisFis, alanKurum, KrediYonetimiStokIslemTipi.IPTAL, kullanici);

    }

    public List<FirmaMusteri> getSatisFirmaMusteriler(Sorgu sorgu, Kurum kurum, MuhatapTip muhatapTip) {
        if (muhatapTip.equals(MuhatapTip.ORTAK_ICI)) {
            sorgu.setSorgulanacakSinif(FirmaMusteri.class);
            sorgu.kriterEkle(KriterFactory.dolu("ortak"));
        } else if ((muhatapTip.equals(MuhatapTip.ORTAK_DISI))) {
            sorgu.setSorgulanacakSinif(FirmaMusteri.class);
            sorgu.kriterEkle(KriterFactory.bos("ortak"));
            sorgu.kriterEkle(KriterFactory.bos("ttkKurum"));
        } else if ((muhatapTip.equals(MuhatapTip.TESKILAT_ICI))) {
            sorgu.setSorgulanacakSinif(TuzelFirmaMusteri.class);
            sorgu.kriterEkle(KriterFactory.dolu("ttkKurum"));
        }

        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));

        return dao.sorgula(sorgu);
    }

    public List<FirmaMusteri> getSatisFirmaMusteriMerkez(Sorgu sorgu, Kurum kurum, Kurum kurumMerkez) {

        sorgu.setSorgulanacakSinif(TuzelFirmaMusteri.class);


        sorgu.kriterEkle(KriterFactory.esit("durum", FirmaMusteriDurumu.AKTIF));
        sorgu.kriterEkle(KriterFactory.esit("ttkKurum", kurumMerkez));
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));

        return dao.sorgula(sorgu);
    }

    public List<Hesap> getSatisMuhatapHesaplar(Sorgu sorgu, Kurum kurum, KoopSatisOdemeSekli odemeSekli, MuhatapTip muhatapTip, VeresiyeTip veresiyeTip, boolean gencCiftciMi, String fm) throws BusinessRuleException {
        sorgu.setSorgulanacakSinif(Hesap.class);

        if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TEK_CEKIM)) {

            muhasebeModulServis.altHesapBulYoksaAc("10822000100001", "KREDİ KARTI TEK ÇEKİM HESABI", kurum);
            sorgu.kriterEkle(KriterFactory.aralik("hesapNo", new Aralik("10822000100001", "10822000100009")));

//            if (muhatapTip.equals(MuhatapTip.ORTAK_ICI)) {
//                muhasebeModulServis.altHesapBulYoksaAc("10822000100001", "KREDİ KARTI TEK ÇEKİM HESABI", kurum);
//                sorgu.kriterEkle(KriterFactory.esit("hesapNo", "10822000100001"));
//
//            } else if (muhatapTip.equals(MuhatapTip.ORTAK_DISI)) {
//                muhasebeModulServis.altHesapBulYoksaAc("10822000200001", "KREDİ KARTI  ORTAK DIŞI TEK ÇEKİM HESABI", kurum);
//                sorgu.kriterEkle(KriterFactory.esit("hesapNo", "10822000200001"));
//
//            }


        } else if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TAKSITLI)) {
            muhasebeModulServis.altHesapBulYoksaAc("10823000100001", "KREDİ KARTI TAKSİTLİ HESABI", kurum);
//            sorgu.kriterEkle(KriterFactory.esit("hesapNo", "10823000100001"));
            sorgu.kriterEkle(KriterFactory.aralik("hesapNo", new Aralik("10823000100001", "10823000100009")));

//
//            if (muhatapTip.equals(MuhatapTip.ORTAK_ICI)) {
//                muhasebeModulServis.altHesapBulYoksaAc("10823000100001", "KREDİ KARTI TAKSİTLİ HESABI", kurum);
//                sorgu.kriterEkle(KriterFactory.esit("hesapNo", "10823000100001"));
////                sorgu.kriterEkle(KriterFactory.benzer("hesapNo", KoopSatisFis.KREDI_KARTI_TAKSITLI_ORTAK_ICI_MUAVIN_HESAP));
//            } else if (muhatapTip.equals(MuhatapTip.ORTAK_DISI)) {
//                muhasebeModulServis.altHesapBulYoksaAc("10823000200001", "KREDİ KARTI TAKSİTLİ ORTAK DIŞI HESABI", kurum);
//                sorgu.kriterEkle(KriterFactory.esit("hesapNo", "10823000200001"));
//
//            }


        } else if (odemeSekli.equals(KoopSatisOdemeSekli.VERESIYE)) {
            if (veresiyeTip.equals(VeresiyeTip.CEK)) {
                sorgu.kriterEkle(KriterFactory.benzer("hesapNo", KoopSatisFis.VERESIYE_CEK_MUAVIN_HESAP + fm));
            }

            if (veresiyeTip.equals(VeresiyeTip.BONO)) {
                sorgu.kriterEkle(KriterFactory.benzer("hesapNo", KoopSatisFis.VERESIYE_MUAVIN_HESAP + fm));
            }


        } else if (odemeSekli.equals(KoopSatisOdemeSekli.MAHSUBEN_ODEMELI)) {
            sorgu.kriterEkle(
                    KriterFactory.or(
                            KriterFactory.benzer("hesapNo", KoopSatisFis.MAHSUBEN_ODEMELI_KOOP_TALI),
                            KriterFactory.benzer("hesapNo", KoopSatisFis.MAHSUBEN_ODEMELI_MUAVIN_HESAP)));
        } else if (odemeSekli.equals(KoopSatisOdemeSekli.VADELI)) {
            if (gencCiftciMi) {
                sorgu.kriterEkle(KriterFactory.benzer("hesapNo", KoopSatisFis.GENC_CIFTCI_MUHATAP_TALI_HESAP_NO + fm));


            } else
                sorgu.kriterEkle(KriterFactory.benzer("hesapNo", KoopSatisFis.VADELI_ODEMELI_MUAVIN_HESAP));
        }


        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));

        return dao.sorgula(sorgu);
    }

    public void koopSatisHareketTutarlariHesapla(KoopSatisFisHareket hareket, KoopSatisFis satisFis) throws BusinessRuleException {
        getSatisFisYonetici().koopTutarHesapla(hareket, satisFis);
    }

    public void bilesenliUrunFisKes(BilesenliUrunFis bilesenliUrunFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(bilesenliUrunFis, kullanici);
    }

    public void bilesenliUrunFisHareketEkle(BilesenliUrunFis bilesenliUrunFis, BilesenliUrunFisHareket bilesenliUrunFisHareket) throws BusinessRuleException {
        getStokFisYonetici().bilesenliUrunFisHareketEkle(bilesenliUrunFis, bilesenliUrunFisHareket);
    }

    public List<BilesenliUrunFis> getBilesenliUrunFisler(Kurum kurum) {
        return dao.getBilesenliUrunFisler(kurum);
    }

    public void bilesenliUrunFisiOnayla(BilesenliUrunFis bilesenliUrunFis) throws BusinessRuleException {
        getStokFisYonetici().bilesenliUrunFisiOnayla(bilesenliUrunFis);
    }

    public StokIptalFis bilesenliUrunFisIptal(BilesenliUrunFis bilesenliUrunFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().bilesenliUrunFisIptal(bilesenliUrunFis, kullanici, kesilenFisler);
    }

    public List<KurumStok> getBilesenliStoklar(Sorgu sorgu) {
        return dao.sorgula(sorgu, "size(e.stok.urunBilesenler) <> 0");
    }

    public void bilesenliUrunGirisTutarHesapla(BilesenliUrunFisHareket stokHareket) {
        BigDecimal tutar = getStokFisYonetici().getOrtalamaMaliyet(stokHareket.getStok()).multiply(stokHareket.getMiktar());
        stokHareket.setTutar(tutar);
    }

    public MahsupFis akaryakitCikisYap(BolgeSatisFis satisFis, AkaryakitIslem akaryakitIslem, Kullanici kullanici, BigDecimal miktar) throws BusinessRuleException {
        return getAkaryakitYonetici().akaryakitCikisYap(satisFis, akaryakitIslem, kullanici, miktar);
    }

    public List<AkaryakitFazlalikGirisFis> getAkaryakitFazlalikGirisFisler(Sorgu sorgu, Kurum aktifKurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", aktifKurum));
        return dao.sorgula(sorgu);
    }

    public StokIptalFis akaryakitFazlalikGirisIptal(AkaryakitFazlalikGirisFis akaryakitFazlalikGirisFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getAkaryakitYonetici().akaryakitFazlalikGirisIptal(akaryakitFazlalikGirisFis, kullanici, kesilenFisler);
    }

    public List<BolgeSatisFis> getBolgeSatisFisler(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public boolean kurumunAcilisFisiVarMi(AcilisFis fis) {
        return dao.kurumunBaskaAcilisFisiVarMi(fis);

    }

    public SatisFiyat getSatisFiyatBolge(KurumStok kurumStok, BolgeSatisFis bolgeSatisFis) throws BusinessRuleException {
        Integer vadegun = bolgeSatisFis.isVadeliSatisMi() ? bolgeSatisFis.getVade() : 0;
        Ilce ilce;
        if (kurumStok.isAkaryakit()) {
            ilce = bolgeSatisFis.getMuhatap().getFaturaIlce() == null ? null : bolgeSatisFis.getMuhatap().getFaturaIlce();
        }

        ilce = null;


        return getSatisFiyatBolge(kurumStok, bolgeSatisFis.getFatura().getFaturaTarihi(), bolgeSatisFis.getBolgeFiyatTip(),
                bolgeSatisFis.getMuhatap().getFaturaIl(), vadegun, ilce);
    }

    public List<FirmaMusteri> getBolgeVeyaKooperatifOlanMusteriler(Sorgu sorgu) {
        return firmaMusteriModulServis.getBolgeVeyaKooperatifOlanMusteriler(sorgu);
    }

    public List<BolgeSatisFisHareket> getBolgeSatisFisHareketler(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public MahsupFis akaryakitIadeYap(AkaryakitIade akaryakitIade, Kullanici kullanici) throws BusinessRuleException {
        return getAkaryakitYonetici().akaryakitIadeYap(akaryakitIade, kullanici);
    }

    public void fisHareketDurdurmaKuraliKaydet(StokFisHareketDurdurmaKurali izin) throws BusinessRuleException {
        kaydet(izin);
    }

    public void cikisIsleminiDurdurmaKuraliKaydet(CikisIslemiDurdurmaKurali izin) throws BusinessRuleException {
        kaydet(izin);
    }

    public void cikisFisHareketleriniDurdur(StokFisHareketDurdurmaKurali cikisIzin) throws BusinessRuleException {
        kaydet(cikisIzin);
    }

    public List<StokFis> topluFaturaNoVerilecekFisler(Sorgu sorgu, KurumStok stok, boolean isMerkez) {
        if (isMerkez) {
            TuzelFirmaMusteri muhatap = (TuzelFirmaMusteri) sorgu.getKriterValue("muhatap");

            if (muhatap != null) {
                sorgu.kriterCikar("muhatap");
                return dao.getFaturaNoVerilecekBolgeyeBagliKoopMuhatapliFaturaliFisler(sorgu, stok, muhatap.getTtkKurum());
            }
        }

        return dao.getFaturaNoVerilecekFaturaliFisler(sorgu, stok);
    }

    public List<TuzelFirmaMusteri> teskilatIciFirmaMusteriler(Kurum kurum) {
        Sorgu sorgu = new Sorgu(TuzelFirmaMusteri.class);
        sorgu.kriterEkle("kurum", kurum);

        if (kurum.getTip().equals(KurumTip.MERKEZ)) {
            sorgu.kriterEkle("ttkKurum.tip", KurumTip.BOLGE);
        }

        if (kurum.getTip().equals(KurumTip.BOLGE)) {
            sorgu.kriterEkle("ttkKurum.tip", KurumTip.KOOPERATIF);
        }

        return sorgula(sorgu);
    }

    public void topluFaturaNoVer(Sorgu sorgu, KurumStok stok, String faturaNo, boolean isMerkez) throws BusinessRuleException {
        Long sayac = new Long(faturaNo);
        List<StokFis> faturaNoVerilecekFisler = topluFaturaNoVerilecekFisler(sorgu, stok, isMerkez);

        for (StokFis fis : faturaNoVerilecekFisler) {
            if (fis instanceof FaturaliStokFis) {
                FaturaliStokFis sfFis = (FaturaliStokFis) fis;
                faturaNo = sayac.toString();
                sfFis.getFatura().setFaturaNo(faturaNo);
                kaydet(sfFis);
                sayac = sayac + 1;
            }
        }

        if (faturaNoVerilecekFisler.isEmpty()) {
            throw new BusinessRuleException(StokHataKodu.TOPLU_FATURA_ISLEMI_YAPILACAK_KAYIT_BULUNAMADI);
        }
    }

    public StokIptalFis muhasebesizIptalFisiOnayla(StokFis stokFis, List<StokFis> kesilenFisler, Kullanici kullanici) throws BusinessRuleException {
        return getStokFisYonetici().muhasebesizIptalFisiOnayla(stokFis, kesilenFisler, kullanici);
    }

    public List<StokFis> getMuhasebesizStokFisler(Sorgu sorgu) {
        return dao.getMuhasebesizStokFisler(sorgu);
    }

    public List<FaturaFisHareket> getFaturaFisHareketler(String isim) {
        return dao.getFaturaFisHareketler(isim);
    }

    public StokFis fisKopyala(StokFis kopyalananStokFis, Tarih fisTarihi) throws BusinessRuleException {
        if (kopyalananStokFis.isGecici()) {
            throw new BusinessRuleException(StokHataKodu.GECICI_FIS_KOPYALANAMAZ);
        }

        StokFis kopyaFis = ReflectionUtils.callSelfArgumentConstructor(kopyalananStokFis);

        if (kopyalananStokFis instanceof FaturaFis) {
            ((FaturaFis) kopyaFis).setKdvBilgileri(new ArrayList<FaturaKdvBilgi>());
        }

        kopyaFis.setFisTarihi(fisTarihi);
        getStokFisYonetici().stokFisKes(kopyaFis, kopyalananStokFis.getKullanici(), kopyalananStokFis.getKurum());

        return kopyaFis;
    }

    public List<SatistanIadeFis> getSatistanIadeFisler(Kurum kurum, Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public List<StokFis> getIptalEdilmemisOnayliSatisFisleri(Sorgu sorgu, Kurum kurum) {
        return dao.getIptalEdilmemisOnayliSatisFisleri(sorgu, kurum);
    }

    public void satistanIadeFisiKes(SatistanIadeFis satistanIadeFis, Kullanici kullanici) throws BusinessRuleException {
        getSatisFisYonetici().satistanIadeFisiKes(satistanIadeFis, kullanici);
    }

    public void satistanIadeFisOnayla(SatistanIadeFis satistanIadeFis) throws BusinessRuleException {
        getSatisFisYonetici().satisIadeFisOnayla(satistanIadeFis);
    }

    public void satistanIadeFisiHareketEkle(SatistanIadeFis satistanIadeFis, SatistanIadeFisHareket satistanIadeFisHareket) throws BusinessRuleException {
        getSatisFisYonetici().satistanIadeFisiHareketEkle(satistanIadeFis, satistanIadeFisHareket);
    }

    public List<Hesap> getYediYuzYetmisHesaplar(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.benzer("hesapNo", "770"));
        return muhasebeModulServis.getKurumaAitAcikAltHesaplar(kurum, sorgu);
    }

    public void uretimFisindenHizmetSil(UretimFis uretimFisi, UretimHizmet uretimHizmet) throws BusinessRuleException {
        getUretimFisYoneticisi().uretimFisindenHizmetSil(uretimFisi, uretimHizmet);
    }

    public void uretimHizmetGuncelle(UretimHizmet uretimHizmet) throws BusinessRuleException {
        getUretimFisYoneticisi().uretimHizmetGuncelle(uretimHizmet);
    }

    public StokIptalFis uretimFisiIptal(UretimFis uretimFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getUretimFisYoneticisi().uretimFisiIptal(uretimFis, kullanici, kesilenFisler);
    }

    public StokFIFAIptalFis alisFiyatIndirimFisIptal(AlisFiyatIndirimFis alisFiyatIndirimFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().alisFiyatIndirimFisIptal(alisFiyatIndirimFis, kullanici, kesilenFisler);
    }

    private boolean satisFiyatTipineGoreStokIskontoDoluMu(StokIskonto stokIskonto, KoopSatisFiyatTip koopSatisFiyatTip) {
        if (koopSatisFiyatTip.equals(KoopSatisFiyatTip.PESIN) && stokIskonto.getPesinIskontoOrani() != null) {
            return true;
        }

        if (koopSatisFiyatTip.equals(KoopSatisFiyatTip.KREDI_KARTI_TAKSITLI) && stokIskonto.getAylikKrediKartiTaksitFarki() != null) {
            return true;
        }

        if (koopSatisFiyatTip.equals(KoopSatisFiyatTip.VADELI) && stokIskonto.getAylikVadeliSatisFarki() != null) {
            return true;
        }

        return false;
    }

    public void konsinyeCikisFisiKes(KonsinyeCikisFis konsinyeCikisFis, Kullanici aktifKullanici) throws BusinessRuleException {
        getKonsinyeFisYonetici().stokFisKes(konsinyeCikisFis, aktifKullanici);
    }

    public void konsinyeCikisFisHareketEkle(KonsinyeCikisFis konsinyeCikisFis, KonsinyeCikisFisHareket konsinyeCikisFisHareket) throws BusinessRuleException {
        getKonsinyeFisYonetici().konsinyeCikisFisHareketEkle(konsinyeCikisFis, konsinyeCikisFisHareket);
    }

    public List<KonsinyeCikisFis> getKonsinyeCikisFisler(Kurum aktifKurum) {
        return dao.sorgula(new Sorgu(KonsinyeCikisFis.class, null, KriterFactory.esit("kurum", aktifKurum)));
    }

    public void konsinyeCikisFiseKdvBilgiEkle(KonsinyeCikisFis konsinyeCikisFis, FaturaKdvBilgi kdvBilgi) throws BusinessRuleException {
        konsinyeCikisFis.kdvBilgiEkle(kdvBilgi);
        kaydet(konsinyeCikisFis);
    }

    public void konsinyeCikisFisOnayla(KonsinyeCikisFis konsinyeCikisFis) throws BusinessRuleException {
        getKonsinyeFisYonetici().konsinyeCikisFisOnayla(konsinyeCikisFis);
    }

    public KrediliSatisFiyat getKrediliSatisFiyat(KurumStok kurumStok, Tarih tarih) throws BusinessRuleException {
        KoopSatisFiyat satisFiyat = getSatisFiyatKoop(kurumStok, tarih, MuhatapTip.ORTAK_ICI);

        StokIskonto bolgeStokIskonto = getStokIskonto(kurumStok.getKurum().getUstKurum(), kurumStok, tarih, KoopSatisFiyatTip.VADELI);

        return new KrediliSatisFiyat(tarih, satisFiyat, bolgeStokIskonto.getAylikVadeliSatisFarki());
    }

    public KrediliSatisFiyat getKrediliSatisFiyat(KurumStok kurumStok, Tarih tarih, KoopSatisFiyat satisFiyat) throws BusinessRuleException {

        StokIskonto bolgeStokIskonto = getStokIskonto(kurumStok.getKurum().getUstKurum(), kurumStok, tarih, KoopSatisFiyatTip.VADELI);

        return new KrediliSatisFiyat(tarih, satisFiyat, bolgeStokIskonto.getAylikVadeliSatisFarki());
    }

    public KrediliSatisFiyat getKrediliSatisFiyat(KoopSatisFiyat satisFiyat, KurumStok kurumStok, Tarih tarih) throws BusinessRuleException {

        StokIskonto bolgeStokIskonto = getStokIskonto(kurumStok.getKurum().getUstKurum(), kurumStok, tarih, KoopSatisFiyatTip.VADELI);

        return new KrediliSatisFiyat(tarih, satisFiyat, bolgeStokIskonto.getAylikVadeliSatisFarki());
    }

    public List<KoopSatisFiyat> getSatisFiyatKooplariForKredi(KurumStok kurumStok, Tarih tarih) throws BusinessRuleException {
        return getSatisFiyatKooplariForKredi(kurumStok, tarih, MuhatapTip.ORTAK_ICI);

    }

    public KoopSatisFis krediliSatisFisiKes(Ortak ortak, Hesap muhatapHesap, Tarih faturaTarihi, Hesap mahsubenTahsilatHesap, BigDecimal mahsubenTahsilatTutari, Ay terminAyi, Integer terminYili, String aciklama, Kullanici kullanici, Kurum kurum, StokFisKaynak fisKaynak) throws BusinessRuleException {
        FirmaMusteri muhatap = firmaMusteriModulServis.getFirmaMusteriByOrtak(ortak);
        if (muhatap == null || muhatap.getId() == Entity.NULL_ID) {
            throw new BusinessRuleException(StokHataKodu.FIRMA_MUHATAP_BOS_OLAMAZ);
        }
        return getSatisFisYonetici().krediliSatisFisiKes(muhatap, muhatapHesap, faturaTarihi, mahsubenTahsilatHesap, mahsubenTahsilatTutari, terminAyi, terminYili, aciklama, kullanici, kurum, fisKaynak);
    }

    public KoopSatisFis krediliSatisFisiOlustur(Ortak ortak, Hesap muhatapHesap, Tarih faturaTarihi, Hesap mahsubenTahsilatHesap, BigDecimal mahsubenTahsilatTutari, Ay terminAyi, Integer terminYili, String aciklama, Kullanici kullanici, Kurum kurum, StokFisKaynak fisKaynak) throws BusinessRuleException {
        FirmaMusteri muhatap = firmaMusteriModulServis.getFirmaMusteriByOrtak(ortak);
        if (muhatap == null || muhatap.getId() == Entity.NULL_ID) {
            throw new BusinessRuleException(StokHataKodu.FIRMA_MUHATAP_BOS_OLAMAZ);
        }
        return getSatisFisYonetici().krediliSatisFisiOlustur(muhatap, muhatapHesap, faturaTarihi, mahsubenTahsilatHesap, mahsubenTahsilatTutari, terminAyi, terminYili, aciklama, kullanici, kurum, fisKaynak);
    }

    public KoopSatisFisHareket krediliSatisFisHareketEkle(KoopSatisFis satisFis, KurumStok kurumStok, TakipOzelligi takipOzellik, BigDecimal miktar, BigDecimal satisFiyat, String aciklama, String ziraiIlacNeden, KrediliSatisFiyat krediliSatisFiyat, EkHizmetBuro ekHizmetBuro, String kulakKupeNo, Tarih tarih, TabanTavanKaldirmaTalep tabanTavanKaldirmaTalep) throws BusinessRuleException {
        //ortak kredi modulunden stok eklerken bu metoda gelmektedir..
        return getSatisFisYonetici().krediliSatisFisHareketEkle(satisFis, kurumStok, takipOzellik, miktar, satisFiyat, aciklama, ziraiIlacNeden, krediliSatisFiyat, ekHizmetBuro, kulakKupeNo, tarih, tabanTavanKaldirmaTalep);
    }

    public Hesap getSatisMaliyetHesap(SatisFisHareket hareket) throws BusinessRuleException {
        return getSatisFisYonetici().getSatisMaliyetHesap((SatisFis) hareket.getStokFis(), hareket);
    }

    public MahsupFis satisFisiOnayla(KoopSatisFis satisFis, MahsupFis mahsupFis, Hesap muhatapHesap, Kullanici kullanici, Tarih vadeBaslangicTarihi, boolean vadeliFaizsizAyniKrediMi, String remoteAdress) throws BusinessRuleException {
        return getSatisFisYonetici().satisFisOnayla(satisFis, mahsupFis, muhatapHesap, kullanici, vadeBaslangicTarihi, vadeliFaizsizAyniKrediMi, remoteAdress);
    }

    public MahsupFis krediliSatisFisiOnayla(KoopSatisFis satisFis, MahsupFis mahsupFis, Hesap muhatapHesap, Kullanici kullanici) throws BusinessRuleException {
        return getSatisFisYonetici().krediliSatisFisOnayla(satisFis, mahsupFis, muhatapHesap, kullanici);
    }

    public void girisFaturaDosyaYukle(InputStream inputStream, Kullanici kullanici, boolean gecmisAyinSonIsGunune) throws BusinessRuleException {
        Scanner scanner = new Scanner(inputStream);

        while (scanner.hasNextLine()) {
            girisFaturaSatirIsle(scanner.nextLine(), kullanici, gecmisAyinSonIsGunune);
        }

        scanner.close();
    }

    protected void girisFaturaSatirIsle(String aLine, Kullanici kullanici, boolean gecmisAyinSonIsGunune) throws BusinessRuleException {
        //use a second Scanner to parse the content of each line
        Scanner scanner = new Scanner(aLine);
        scanner.useDelimiter("&");
        if (scanner.hasNext()) {

            String kurumNo = scanner.next();
            String faturaSeriNo = scanner.next();
            String faturaNo = scanner.next();
            if (!canEnterStokCriticalSection(kullanici.getAktifKurum(), "FaturaYukleme", faturaNo, true)) {
                throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
            }

            String faturaTarihi = scanner.next();
            String faturaTutari = scanner.next();
            String terminAyi = scanner.next();
            String terminYili = scanner.next();
            String muhatap = scanner.next();
            String valorTarihi = scanner.next();
            String miktar = scanner.next();
            String kurumStokNo = scanner.next();
            String kdvTutar = scanner.next();
            kdvTutar = kdvTutar.trim();
//            String sevkSekli = scanner.next();
//            sevkSekli=sevkSekli.trim();

//            FirmaMusteri firmaMusteri = (FirmaMusteri) getDao().sorgula(new Sorgu(FirmaMusteri.class, null, KriterFactory.esit("muhasebeHesapNo", muhatap))).get(0);
            FirmaMusteri firmaMusteri = yukle(FirmaMusteri.class, Long.valueOf(muhatap).longValue());
//

            FaturaFis faturaFis = new FaturaFis();
            faturaFis.setFatura(new Fatura(faturaSeriNo, faturaNo, new Tarih(faturaTarihi), new BigDecimal(faturaTutari)));

            if (gecmisAyinSonIsGunune) {
                faturaFis.setFisTarihi(getOncekiAyinSonIsgunu(kullanici.getAktifKurum()));

            } else {
                faturaFis.setFisTarihi(getSimdikiTarih());
            }

            faturaFis.setTerminAyi(Ay.kodOf((new Integer(terminAyi)).toString()));
            faturaFis.setTerminYili(new Integer(terminYili));
            faturaFis.setMuhatap(firmaMusteri);
            faturaFis.setValorTarihi(new Tarih(valorTarihi));

//            SevkSekli sevkSekliEnum=SevkSekli.koddanBul(sevkSekli);
//            faturaFis.setAciklama(sevkSekliEnum.getEtiket()+"---veri aktarımı");
//


            faturaFis.setAciklama("veri aktarımı");
            faturaFis.setVeridenMi(true);
            faturaFis.setTeslimTarihi(new Tarih(faturaTarihi));
            faturaFis.setFisKonuAyrac(FisKonuAyrac.GUBRE);
            faturaFisKes(faturaFis, kullanici);

            Kurum kurum = (Kurum) sorgula(new Sorgu(Kurum.class, null, KriterFactory.esit("kurumNo", kurumNo))).get(0);

            KurumStok kurumStok = getKurumStoklari(new Sorgu(KurumStok.class, null, KriterFactory.esit("stok.stokKodu", kurumStokNo)), kurum).get(0);

            BigDecimal tutar = new BigDecimal(faturaTutari).subtract(new BigDecimal(kdvTutar));
            FaturaFisHareket faturaFisHareket = new FaturaFisHareket(kurumStok, null,
                    null, null, new BigDecimal(miktar), tutar,
                    new BigDecimal(miktar), "hareket");


            FaturaKdvBilgi faturaKdvBilgi = new FaturaKdvBilgi();
            faturaKdvBilgi.setOran(kurumStok.getAlisKdvOrani());
            faturaKdvBilgi.setTutar(new BigDecimal(kdvTutar));
            faturaFis.getKdvBilgileri().add(faturaKdvBilgi);
            faturaFisHareketEkle(faturaFis, faturaFisHareket);

            getStokFisYonetici().hareketlerideKapaliVeAyYilDuzenle(faturaFis);


            fisOnayla(faturaFis);
        }

        scanner.close();
    }

    public void satisFiyatDosyaYukle(InputStream inputStream, Kullanici kullanici) throws BusinessRuleException {
        Scanner scanner = new Scanner(inputStream);

        while (scanner.hasNextLine()) {
            satisFiyatSatirIsle(scanner.nextLine(), kullanici);
        }

        scanner.close();
    }

    public List<FaturaliStokFis> getKurumaKesilenTeskilatIciStokAktarimlar(Sorgu sorgu) {
        return dao.getKurumaKesilenTeskilatIciStokAktarimlar(sorgu);
    }

    protected void satisFiyatSatirIsle(String aLine, Kullanici kullanici) throws BusinessRuleException {
        //use a second Scanner to parse the content of each line
        Scanner scanner = new Scanner(aLine);
        scanner.useDelimiter("&");
        if (scanner.hasNext()) {
            String kurumNo = scanner.next();
            String kurumStokNo = scanner.next();
            String gecerliTarih = scanner.next();
            String fiyat = scanner.next();
            String bolgeSatisFiyatTip = scanner.next();
            String vadeGunSayisi = scanner.next();
            String gecerliIl = scanner.next().trim();

            Kurum kurum = (Kurum) sorgula(new Sorgu(Kurum.class, null, KriterFactory.esit("kurumNo", kurumNo))).get(0);
            KurumStok kurumStok = getKurumStoklari(new Sorgu(KurumStok.class, null, KriterFactory.esit("stok.stokKodu", kurumStokNo)), kurum).get(0);
            Il il = commonServis.getIlByIlkodu(gecerliIl.trim());

            BolgeSatisFiyat bolgeSatisFiyat = new BolgeSatisFiyat();
            bolgeSatisFiyat.setFiyat(new BigDecimal(fiyat));
            bolgeSatisFiyat.setFiyatTipi(BolgeSatisFiyatTip.kodOf(bolgeSatisFiyatTip));
            bolgeSatisFiyat.setGecerliIl(il);
            bolgeSatisFiyat.setGecerliTarih(new Tarih(gecerliTarih));
            bolgeSatisFiyat.setKurumStok(kurumStok);
            if (!vadeGunSayisi.equals("0")) {
                bolgeSatisFiyat.setVadeGunSayisi(new Integer(vadeGunSayisi));
            }
            kaydet(bolgeSatisFiyat);
        }

        scanner.close();
    }


    public SatisIptalFis satisIptalFisiOlustur(SatisFis satisFis, Kullanici kullanici, StokFisKaynak fisKaynak) throws BusinessRuleException {
        return getSatisFisYonetici().satisIptalFisiOlustur(satisFis, kullanici, fisKaynak);
    }


    public List<KonsinyeIadeFis> getKonsinyeIadeFisler(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public List<KurumStok> getKonsinyeler(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        sorgu.kriterEkle(KriterFactory.dolu("stok.konsinyeMuhasebeHesabi"));

        return dao.sorgula(sorgu);
    }

    public boolean oncekiAyaFisKesilebilir(Kurum kurum) {
        return muhasebeModulServis.oncekiAyaFisKesilebilir(getSimdikiTarih(), kurum);
    }

    public Tarih getOncekiAyinSonIsgunu(Kurum kurum) {
//        return takvim.getOncekiAyinSonIsGunu(kurum);
        return takvim.getGecenAyinSonGunu();
    }

    public Tarih getOncekiAyinSonIsgunu() {
//        return takvim.getOncekiAyinSonIsGunu(kurum);
        return takvim.getGecenAyinSonGunu();
    }

    public List<AkaryakitFazlalikMiktarListData> getAkaryakitFazlaliklar(Sorgu sorgu, Kurum kurum) {
        return getAkaryakitYonetici().getAkaryakitFazlaliklar(sorgu, kurum);
    }

    public AkaryakitIslem getAkaryakitIslem(FaturaFisHareket faturaFisHareket) throws BusinessRuleException {
        return getAkaryakitYonetici().getAkaryakitIslemYoksaOlustur(faturaFisHareket);
    }

    public List<AkaryakitIslem> getAkaryakitIslemler(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public void muhasebesizStokFisMuhasebelestir(StokFis stokFis) throws BusinessRuleException {
        String metodAd = stokFis.getTipName().substring(0, 1).toLowerCase() + stokFis.getTipName().substring(1) + "Onayla";
        Method stokFisOnaylaMethod = metoduBul(metodAd, new Class[]{stokFis.getClass()});

        metodCalistir(stokFisOnaylaMethod, new Object[]{stokFis});
    }

    private Method metoduBul(String metodAdi, Class[] paramTypes) throws BusinessRuleException {
        try {
            return getClass().getMethod(metodAdi, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new BusinessRuleException(StokHataKodu.METOD_BULUNAMADI, e);
        }
    }

    private Object metodCalistir(Method metod, Object[] params) throws BusinessRuleException {
        try {
            return metod.invoke(this, params);
        } catch (IllegalAccessException e) {
            throw new BusinessRuleException(StokHataKodu.METOD_BULUNAMADI, e);
        } catch (InvocationTargetException e) {
            throw new BusinessRuleException(StokHataKodu.METOD_BULUNAMADI, e);
        }
    }

    public void muhasebesizStokIptalFisMuhasebelestir(StokIptalFis stokIptalFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        if (stokIptalFis.getKurum().isMerkez()) {
            merkezIptalFisMuhasebelestir(stokIptalFis, kullanici);
        }

//        StokFis stokFis = stokIptalFis.getIptalEdilenFis();
//
//        String metodAd = stokFis.getTipName().substring(0, 1).toLowerCase() + stokFis.getTipName().substring(1) + "Iptal";
//        Class[] paramTypes = {Hibernate.getClass(stokFis), kullanici.getClass(), List.class};
//
//        Method stokFisOnaylaMethod = metoduBul(metodAd, paramTypes);
//
//        Entity stokFis2 = yukle(Hibernate.getClass(stokFis), stokFis.getId());
//
//        Object[] parameters = {stokFis2, kullanici, kesilenFisler};
//
//        metodCalistir(stokFisOnaylaMethod, parameters);

//        sil(stokIptalFis);
    }

    private void merkezIptalFisMuhasebelestir(StokIptalFis stokIptalFis, Kullanici kullanici) throws BusinessRuleException {
        getSatisFisYonetici().merkezIptalFisMuhasebelestir(stokIptalFis, kullanici);


    }


    public void otomatikIslemlerStokFisSil(StokFis fis) {
        kontrolPesinKrediliSatisVeSil(fis);
        kontrolSulamaMalzemeVeSil(fis);
        if (fis.getKaynak().equals(StokFisKaynak.STOK) && fis.isVeridenMi() == true && fis.getTip().equals(StokFisTip.FATURA)) {
            kontrolOtomotikOlusanSevkStokFisiVeSil(fis);
        }
        dao.sil(fis);
    }

    private void kontrolOtomotikOlusanSevkStokFisiVeSil(StokFis fis) {
        //sevk  fişlerindeki tanımlı fatura fis bosaltiliyor
        if (fis.getAciklama().substring(0, 3).equals("MB/")) {
            Sorgu sorgu = new Sorgu(SevkStokCikisFis.class);
            sorgu.kriterEkle(KriterFactory.esit("girisFaturaFis", fis));
            List<SevkStokCikisFis> sevkStokCikisFisler = dao.sorgula(sorgu);
            for (SevkStokCikisFis sevkStokCikisFis : sevkStokCikisFisler) {
                sevkStokCikisFis.setGirisFaturaFis(null);
                dao.kaydet(sevkStokCikisFis);

            }
            //kurumlararasi olusan satis fişlerindeki   fişlerindeki tanımlı fatura fis bosaltiliyor
        } else {
            Sorgu sorgu = new Sorgu(SatisFis.class);
            sorgu.kriterEkle(KriterFactory.esit("otomotikFaturaFis", fis));
            List<SatisFis> satisFisler = dao.sorgula(sorgu);
            for (SatisFis satisFis : satisFisler) {
                satisFis.setOtomotikFaturaFis(null);
                dao.kaydet(satisFis);

            }
        }

    }

    private void kontrolSulamaMalzemeVeSil(StokFis fis) {
        Sorgu sorgu = new Sorgu(SulamaSistemMalzeme.class);
        sorgu.kriterEkle(KriterFactory.esit("stokFis", fis));
        List<SulamaSistemMalzeme> sulamMalzemeler = dao.sorgula(sorgu);
        for (SulamaSistemMalzeme sulamaSistemMalzeme : sulamMalzemeler) {
            dao.sil(sulamaSistemMalzeme);
        }
    }

    private void kontrolPesinKrediliSatisVeSil(StokFis fis) {
        Sorgu sorgu = new Sorgu(PesinKrediliSatis.class);
        sorgu.kriterEkle(KriterFactory.esit("stokFis", fis));
        List<PesinKrediliSatis> pesinKrediliSatislar = dao.sorgula(sorgu);
        for (PesinKrediliSatis pesinKrediliSatis : pesinKrediliSatislar) {
            dao.sil(pesinKrediliSatis);
        }
    }

    public void stokFisSil(StokFis fis) throws BusinessRuleException {
        dao.fisinPesinKrediliSatisBilgileriniSil(fis);

        if (fis instanceof BolgeSatisFis) {
            BolgeSatisFis bolgeSatisFis = (BolgeSatisFis) fis;

            if (bolgeSatisFis.getAkaryakitIslem() != null) {
                AkaryakitIslem akaryakitIslem = bolgeSatisFis.getAkaryakitIslem();
                akaryakitIslem.satisFisSil(bolgeSatisFis);
                dao.kaydet(akaryakitIslem);
                return;
            }
        }

        silVersiyonlu(fis);
    }

    public void stokKartiOnayla(Stok stok, Kullanici aktifKullanici) throws BusinessRuleException {
        if (stok.getOnaylanmis() || stok.getStokKodu() != null) {
            throw new RuntimeException("Onaylanacak olan stoğun stok kodu olmamalı ve onaylanmış alanı false olmalıdır");
        }

        if (stok.getStokMuhasebeHesabi() == null) {
            throw new BusinessRuleException(StokHataKodu.STOK_MUHASEBE_HESABI_TANIMLANMAMIS_STOK_ONAYLANAMAZ);
        }

        if (stok.getKonsinyeMuhasebeHesabi() == null && stok.getStokMuhasebeHesabi().getHesapNo().startsWith("153") &&
                !stok.getGrup().getKod().startsWith("500")) {
            throw new BusinessRuleException(StokHataKodu.FIRMA_MUHATAP_KONSINYE_HESAP_TANIMLI_DEGIL);
        }


        stok.setStokKodu(getStokKodu());
        stok.setOnaylanmis(true);

        kaydet(stok);

        if (stok.getStokKartiTalep() != null)
            stokKartiTalepBilgileriniGuncelle(stok, aktifKullanici);
    }

    private void stokKartiTalepBilgileriniGuncelle(Stok stok, Kullanici kullanici) throws BusinessRuleException {
        StokKartiTalep stokKartiTalep = yukle(StokKartiTalep.class, stok.getStokKartiTalep().getId());
        stokKartiTalep.getUrunFiyat().setStok(stok);
        stokKartiTalep.getUrunFiyat().setStokKodu(stok.getStokKodu());
        stokKartiTalep.getUrunFiyat().setUrunIsmi(stok.getStokAdi());
        stokKartiTalep.setOnaylimi(true);
        stokKartiTalep.setOnayTarihi(getSimdikiTarih());
        stokKartiTalep.setOnaylayanKullanici(kullanici);
        kaydet(stokKartiTalep);
        kaydet(stokKartiTalep.getUrunFiyat());

    }

    public void kurumStokkaydet(KurumStok kurumStok) throws BusinessRuleException {
        if ((kurumStok.getSatisIskontoTutari() != null && kurumStok.getSatisIskontoYuzde() != null)) {
            throw new BusinessRuleException(StokHataKodu.SATIS_ISKONTO_TUTAR_YADA_YUZDE_ALANLARINDAN_BIRISI_DOLU_OLMALIDIR);
        }

        if (!kurumStok.getAktif()) {
            BigDecimal mevcutMiktar = getMevcutStokMiktari(kurumStok);
            if (EkoopUtils.isBuyuk(mevcutMiktar, BigDecimal.ZERO)) {
                throw new BusinessRuleException(StokHataKodu.BAKIYESI_OLAN_STOK_PASIF_YAPILAMAZ);
            }
        }

        if (kurumStok.isNew()) {
            Sorgu sorguCheck = new Sorgu(KurumStok.class);
            sorguCheck.kriterEkle(KriterFactory.esit("stok", kurumStok.getStok()));
            List<KurumStok> kurumStoks = getKurumStoklari(sorguCheck, kurumStok.getKurum());
            if (kurumStoks.size() > 0) {
                throw new BusinessRuleException(StokHataKodu.BU_KURUM_STOK_DAHA_ONCE_TANIMLANMIS);
            }
        }

        stokTalepKurumKontrol(kurumStok);
        kaydet(kurumStok);
    }

    private void stokTalepKurumKontrol(KurumStok kurumStok) throws BusinessRuleException {
        boolean sonuc = false;
        boolean merkezTalebMi = false;
        if (kurumStok.getStok().getMerkeziAnlasmaMi()) {
            sonuc = true;
        }
        if (!kurumStok.getStok().getMerkeziAnlasmaMi()) {
            for (StokTalebKurum talepKurum : kurumStok.getStok().getStokTalebKurumlar()) {
                if (talepKurum.getKurum().isMerkez()) {
                    merkezTalebMi = true;

                }
                if (kurumStok.getKurum().isMerkez() || kurumStok.getKurum().isBolge()) {
                    if (talepKurum.getKurum().getId() == kurumStok.getKurum().getId()) {
                        sonuc = true;
                    }
                } else {
                    if (talepKurum.getKurum().getId() == kurumStok.getKurum().getUstKurum().getId()
                            || talepKurum.getKurum().getId() == kurumStok.getKurum().getId()) {
                        sonuc = true;
                    }
                }
            }
        }

        if (merkezTalebMi) {
            throw new BusinessRuleException(StokHataKodu.MERKEZ_BIRLIGI_BU_STOKU_KULALNIMINA_KAPATMISTIR);
        }

        if (!sonuc) {
            throw new BusinessRuleException(StokHataKodu.BU_STOK_ANLASMA_KAPSAMINDA_DEGIL);
        }

    }

    public List<FirmaMusteri> kurumaAitFirmaMusterileriBul(Kurum kurum, List<Kriter> kriterler) {
        return dao.kurumaAitFirmaMusterileriBul(kurum, kriterler);
    }

    public StokFis getMuhasebeFisineKarsilikGelenStokFisi(Fis fis) {
        return dao.getMuhasebeFisineKarsilikGelenStokFisi(fis);
    }

    public boolean ortagaBirSeneIcindePesinSatisYapilmisMi(Ortak ortak, Tarih tarih) throws BusinessRuleException {
        return dao.ortagaTarihAraligindaPesinSatisYapilmisMi(ortak, tarih.yilCikar(1), tarih);
    }

    // Mahsup oluşturmadan fişi kapat.
    public void fisOnayla(StokFis stokFis) throws BusinessRuleException {
//        merkezde yilbasi fisi kontrolu konuldu
        if (stokFis.getFisTarihi().getYil() == getSimdikiTarih().getYil()) {
            if (!dao.stokYilbasiDevirFisiVarMi(stokFis.getKurum())) {
                throw new BusinessRuleException(StokHataKodu.YILBASI_STOK_DEVIR_FISINIZ_YOKTUR);
            }
        }

        if (stokFis instanceof FaturaFis) {
            getFaturaFisYoneticisi().validateOnayla(stokFis, false);
        } else if (stokFis instanceof KonsinyeGirisFis || stokFis instanceof KonsinyeCikisFis || stokFis instanceof KonsinyeIadeFis) {
            getKonsinyeFisYonetici().validateOnayla(stokFis, false);
        } else if (stokFis instanceof MustahsilMakbuzuFis) {
            getMustahsilMahbuzuFisYoneticisi().validateOnayla(stokFis, false);
        } else if (stokFis instanceof SatisFis) {
            raporParametreSetForMerkezBirligi(stokFis);
            getSatisFisYonetici().validateOnayla(stokFis, true);
        } else if (stokFis instanceof UretimFis) {
            getUretimFisYoneticisi().validateOnayla(stokFis, false);
        } else if (stokFis instanceof BilesenliUrunFis) {
            getStokFisYonetici().validateOnayla(stokFis, true);
        } else if (stokFis instanceof FireCikisFis) {
            getStokFisYonetici().validateOnayla(stokFis, true);
        } else if (stokFis instanceof MasrafaCikisFis) {
            getStokFisYonetici().validateMasrafaCikisForMerkez((MasrafaCikisFis) stokFis);
        } else if (stokFis instanceof NoksanlikFis) {
            getStokFisYonetici().validateOnayla(stokFis, true);
        } else {
            getSatisFisYonetici().validateOnayla(stokFis, false);
        }

        if (stokFis.getFisTarihi().getAy() != stokFis.getAy()) {
            stokFis.setAy(stokFis.getFisTarihi().getAy());
        }

        stokFis.setKapali(true);
        getStokFisYonetici().hareketlerideKapaliVeAyYilDuzenle(stokFis);
        stokFis.setFisNo(getKaliciFisNo(stokFis.getFisTarihi(), stokFis.getKurum()));
        kaydet(stokFis);

        //TODO 18.12.2018 versiyon icin bypass edildi tekrar devreye alacagiz
//        fisKapat(stokFis,stokFis.getKullanici());


    }

    private void raporParametreSetForMerkezBirligi(StokFis stokFis) {
        if (stokFis.getKurum().isMerkez()) {
            List<BolgeSatisFisHareket> bolgeSatisFisHarekets = stokFis.getHareketler();
            for (BolgeSatisFisHareket hareket : bolgeSatisFisHarekets) {
                BigDecimal kdvSizSatis;
                if (hareket.getStok().isAkaryakit() && hareket.getStokFis().getKurum().isBolge()) {
                    kdvSizSatis = hareket.getTutar();
                } else {
                    kdvSizSatis = hareket.getIndirimsizKdvsizFiyat();
                }
                hareket.setRaporSatis(kdvSizSatis);
                hareket.setRaporBirimFiyat(EkoopUtils.bolVeYuvarla(kdvSizSatis, hareket.getMiktar(), 5));
                hareket.setAlacakTutar(EkoopUtils.yuvarla(hareket.getMaliyet(), 2));
            }
        }
    }

    public void fisKapat(StokFis stokFis, Kullanici kullanici) throws BusinessRuleException {
        //Lazy hatasından dolayı bunu yapıyoruz...
        stokFis = yukle(StokFis.class, stokFis.getId());

        if (stokFis instanceof AlisFiyatArttirimFis) {
            alisFiyatArttirimFisOnayla((AlisFiyatArttirimFis) stokFis);
        } else if (stokFis instanceof AlisFiyatIndirimFis) {
            alisFiyatIndirimFisOnayla((AlisFiyatIndirimFis) stokFis);
        } else if (stokFis instanceof AlistanIadeCikisFis) {
            alistanIadeCikisFisiOnayla((AlistanIadeCikisFis) stokFis);
        } else if (stokFis instanceof BilesenliUrunFis) {
            bilesenliUrunFisiOnayla((BilesenliUrunFis) stokFis);
        } else if (stokFis instanceof FaturaFis) {
            faturaFisOnayla((FaturaFis) stokFis);
        } else if (stokFis instanceof FaturaliNakliyeHamaliyeFis) {
            faturaliNakliyeHamaliyeFisiOnayla((FaturaliNakliyeHamaliyeFis) stokFis);
        } else if (stokFis instanceof FazlalikFis) {
            fazlalikFisOnayla((FazlalikFis) stokFis);
        } else if (stokFis instanceof FireCikisFis) {
            fireCikisFisiOnayla((FireCikisFis) stokFis);
        } else if (stokFis instanceof GiderMakbuzuFis) {
            giderMakbuzFisOnayla((GiderMakbuzuFis) stokFis);
        } else if (stokFis instanceof KonsinyeCikisFis) {
            konsinyeCikisFisOnayla((KonsinyeCikisFis) stokFis);
        } else if (stokFis instanceof KonsinyeGirisFis) {
            konsinyeGirisFisOnayla((KonsinyeGirisFis) stokFis);
        } else if (stokFis instanceof KonsinyeIadeFis) {
            konsinyeCikisFisOnayla((KonsinyeIadeFis) stokFis);
        } else if (stokFis instanceof KoopSatisFis) {
            satisFisOnayla((SatisFis) stokFis, kullanici);
        } else if (stokFis instanceof MasrafaCikisFis) {
            masrafaCikisFisiOnayla((MasrafaCikisFis) stokFis);
        } else if (stokFis instanceof MustahsilMakbuzuFis) {
            mustahsilMakbuzuFisOnayla((MustahsilMakbuzuFis) stokFis);
        } else if (stokFis instanceof NoksanlikFis) {
            noksanlikFisOnayla((NoksanlikFis) stokFis);
        } else if (stokFis instanceof SatisFiyatArttirimFis) {
            satisFiyatArttirimFisOnayla((SatisFiyatArttirimFis) stokFis, kullanici);
        } else if (stokFis instanceof SatistanIadeFis) {
            satistanIadeFisOnayla((SatistanIadeFis) stokFis);
        } else if (stokFis instanceof SigortaFis) {
            sigortaFisOnayla((SigortaFis) stokFis);
        } else if (stokFis instanceof StokIciAktarimFis) {
            fisOnayla(stokFis);
        } else if (stokFis instanceof UretimFis) {
            uretimFisiOnayla((UretimFis) stokFis, null);
        } else if (stokFis instanceof FaturaliMasrafFis) {
            faturaliMasrafFisiOnayla((FaturaliMasrafFis) stokFis);
        }
        else if (stokFis instanceof BolgeSatisFis) {
            bolgeSatisFisOnayla((BolgeSatisFis) stokFis, kullanici);
        }else {
            throw new IllegalArgumentException("BUG: Bu fiş türü için kapatma işlemi tanımlanmamış.");
        }
    }

    public List<StokGrup> getUstGrupOlabilecekStokGruplar(Sorgu sorgu, StokGrup stokGrup) {
        if (stokGrup != null) {
            sorgu.kriterEkle(KriterFactory.esitDegil("kod", stokGrup.getKod()));
        }

        List<StokGrup> ustGrupOlabilecekStokGruplar = new ArrayList<StokGrup>();

        for (StokGrup grup : dao.<StokGrup>sorgula(sorgu)) {
            if (stokGrup == null || !stokGrup.isUstGrupOf(grup)) {
                ustGrupOlabilecekStokGruplar.add(grup);
            }

            // TODO: Neden?
            dao.evict(grup);
        }

        return ustGrupOlabilecekStokGruplar;
    }

    public BigDecimal getOrtalamaMaliyet(KurumStok kurumStok) {
        return getStokFisYonetici().getOrtalamaMaliyet(kurumStok);
    }

    public void sevkAktarimFisiIcinSatisFisiKes(Kullanici kullanici, BolgeSatisFis satisFis) throws BusinessRuleException {
        getSatisFisYonetici().sevkAktarimFisiIcinSatisFisiKes(kullanici, satisFis);
    }

    public List<Stok> getGrubunOnayliStoklari(StokGrup stokGrup) {
        return dao.getGrubunOnayliStoklari(stokGrup);
    }

    private StokIskonto getStokIskontoByKurumStok(Kurum bolge, KurumStok kurumStok, Tarih tarih) {
        Sorgu bolgeStokSorgu = new Sorgu(KurumStok.class);
        bolgeStokSorgu.kriterEkle(KriterFactory.esit("stok", kurumStok.getStok()));
        List<KurumStok> bolgeKurumStoklar = getKurumStokKartlari(kurumStok.getKurum().getUstKurum(), bolgeStokSorgu);
        if (bolgeKurumStoklar.size() == 0) {
            return null;
        }
        return dao.getStokIskontoByKurumStok(bolge, bolgeKurumStoklar.get(0), tarih);
    }

    public List<Hesap> getMahsubenTahsilatHesaplar(Sorgu sorgu, Kurum kurum, MuhatapTip muhatapTip) {
        sorgu.setSorgulanacakSinif(Hesap.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        if (muhatapTip.equals(MuhatapTip.ORTAK_ICI)) {
            sorgu.kriterEkle(KriterFactory.or(KriterFactory.or(KriterFactory.or(KriterFactory.or(KriterFactory.or(KriterFactory.benzer("hesapNo", "102100001"), KriterFactory.benzer("hesapNo", "326")), KriterFactory.benzer("hesapNo", "329")), KriterFactory.benzer("hesapNo", "340")), KriterFactory.benzer("hesapNo", "331707000")), KriterFactory.aralik("hesapNo", new Aralik("33190000100001", "33190999999999"))));
        } else {
            sorgu.kriterEkle(KriterFactory.or(KriterFactory.or(KriterFactory.or(KriterFactory.or(KriterFactory.or(KriterFactory.or(KriterFactory.benzer("hesapNo", "102100001"), KriterFactory.benzer("hesapNo", "1203000")), KriterFactory.benzer("hesapNo", "326")), KriterFactory.benzer("hesapNo", "329")), KriterFactory.benzer("hesapNo", "340")), KriterFactory.benzer("hesapNo", "331707000")), KriterFactory.aralik("hesapNo", new Aralik("33190000100001", "33190999999999"))));
        }


        return dao.sorgula(sorgu);

    }

    public void acilisFisOnayla(AcilisFis acilisFis) throws BusinessRuleException {
        getStokFisYonetici().acilisFisiOnayla(acilisFis);

    }

    public List<Stok> getKurumdaTanimlanmisStokKartlari(Sorgu sorgu, Kurum kurum) {
        List<Stok> stoklar = new ArrayList();
        Sorgu yeniSorgu = new Sorgu(KurumStok.class);
        for (Kriter k : sorgu.getKriterler()) {

            if (k.getOperator() == Operator.ESIT) {
                yeniSorgu.kriterEkle(KriterFactory.esit("stok." + k.getFieldName(), k.getValue()));
            } else if (k.getOperator() == Operator.BENZER) {
                yeniSorgu.kriterEkle(KriterFactory.benzer("stok." + k.getFieldName(), k.getValue()));
            } else if (k.getOperator() == Operator.ICEREN) {
                yeniSorgu.kriterEkle(KriterFactory.iceren("stok." + k.getFieldName(), k.getValue()));
            }
        }
        for (KurumStok kurumStok : getKurumStokKartlari(kurum, yeniSorgu)) {
            stoklar.add(kurumStok.getStok());
        }
        return stoklar;
    }

    public StokBirim getStokBirim(Birim birim) {
        Sorgu sorgu = new Sorgu(StokBirim.class);
        sorgu.kriterEkle(KriterFactory.esit("birim", birim));
        return dao.sorgula(sorgu).size() > 0 ? (StokBirim) dao.sorgula(sorgu).get(0) : null;
    }

    public SatisFiyatArttirimFis getSatisFiyatArtirimFis(KurumStok kurumStok, BigDecimal miktar, BigDecimal farkBirimFiyat, Fatura fatura, FirmaMusteri muhatap, Kullanici kullanici, StokFisKaynak kaynak, String aciklama) throws BusinessRuleException {
        SatisFiyatArttirimFis satisFiyatArttirimFis = new SatisFiyatArttirimFis(fatura, muhatap);
        satisFiyatArttirimFis.setKaynak(kaynak);
        SatisFiyatArttirimFisHareket satisFiyatArttirimFisHareket = new SatisFiyatArttirimFisHareket(kurumStok, miktar, farkBirimFiyat, aciklama);
        satisFiyatArttirimFis.validate();
        satisFiyatArttirimFisHareket.setKdvTutar(satisFiyatArttirimFisHareket.kdvTutariHesapla());
        satisFiyatArttirimFisHareket.setTutar(satisFiyatArttirimFisHareket.kdvsizTutarHesapla());
        fiyatArttirimFisHareketEkle(satisFiyatArttirimFis, satisFiyatArttirimFisHareket);
        getStokFisYonetici().stokFisKes(satisFiyatArttirimFis, kullanici);
        return satisFiyatArttirimFis;

    }

    public void akaryakitCikislariBitir(AkaryakitIslem akaryakitIslem, Kullanici kullanici) throws BusinessRuleException {

        if (BigDecimal.ZERO == akaryakitIslem.getToplamCikisMiktari()) {
            throw new BusinessRuleException(StokHataKodu.HIC_CIKIS_YAPILMADIGINDAN_CIKIS_BITIRILEMEZ);
        } else {

            if (EkoopUtils.isKucuk(akaryakitIslem.getToplamCikisMiktari(), akaryakitIslem.getGirisMiktar())) {
                akaryakitNoksanlikFisiKes(kullanici, akaryakitIslem);

            }
            akaryakitIslem.setCikislarBitti(true);
            kaydet(akaryakitIslem);
        }


    }

    private void akaryakitNoksanlikFisiKes(Kullanici kullanici, AkaryakitIslem akaryakitIslem) throws BusinessRuleException {
        BigDecimal noksanlikMiktar = akaryakitIslem.getGirisMiktar().subtract(akaryakitIslem.getToplamCikisMiktari());
        NoksanlikFis noksanlikFis = new NoksanlikFis();
        noksanlikFis.setFisTarihi(getSimdikiTarih());
        noksanlikFis.setAciklama(akaryakitIslem.getGirisHareket().getStokFis().getFisNo() + " NOLU GIRIS FİŞİNİN AKARYAKIT NOKSANLIK FİŞİ");
        noksanlikFisiKes(noksanlikFis, kullanici);
        BigDecimal tutar = EkoopUtils.tutarCarp(akaryakitIslem.getGirisBirimFiyat(), noksanlikMiktar);
        NoksanlikFisHareket noksanlikFisHareket = new NoksanlikFisHareket(akaryakitIslem.getStok(), noksanlikMiktar, tutar, "akaryakit noksanlik", StokHareketTip.CIKIS);
        noksanlikFisHareket.setAlacakTutar(noksanlikFisHareket.getTutar());
        noksanlikFisHareket.setTutar(null);
        noksanlikFis.hareketEkle(noksanlikFisHareket);
        bolgeAkaryakitNoksanlikFisOnayla(noksanlikFis);

    }

    private void bolgeAkaryakitNoksanlikFisOnayla(NoksanlikFis noksanlikFis) throws BusinessRuleException {
        getStokFisYonetici().bolgeAkaryakitNoksanlikFisOnayla(noksanlikFis);
    }

    public void muhasebesizStokFisiIptalEt(String stokFisNo, Kullanici kullanci) throws BusinessRuleException {
        if (stokFisNo.substring(0, 1) == "G") {
            throw new BusinessRuleException(StokHataKodu.GECICI_FIS_IPTAL_EDILEMEZ);
        }

        StokFis stokFis = getStokFisiByNo(stokFisNo, kullanci.getAktifKurum());
        if (stokFis.getIptalEdenFis() != null) {
            throw new BusinessRuleException(StokHataKodu.BU_FIS_IPTAL_EDILMISTIR);
        }
        getStokFisYonetici().muhasebesizIptalFisiOlustur(stokFis, kullanci);


    }

    public StokFis getStokFisiByNo(String stokFisNo, Kurum kurum) {
        Sorgu sorgu = new Sorgu(StokFis.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        sorgu.kriterEkle(KriterFactory.esit("fisNo", stokFisNo));
        return (StokFis) dao.sorgula(sorgu).get(0);

    }

    public MahsupFis masarafaAkaryakitCikisYap(MasrafaCikisFis masrafaCikisFis, AkaryakitIslem akaryakitIslem, Kullanici kullanici, BigDecimal miktar) throws BusinessRuleException {
        return getAkaryakitYonetici().masrafaAkaryakitCikisYap(masrafaCikisFis, akaryakitIslem, kullanici, miktar);
    }

    public List<StokTakipOzellikDurum> krediTakipOzellikSorgula(Sorgu sorgu, String stokIdler, Kurum kurum) {
        return dao.krediTakipOzellikSorgula(sorgu, kurum, stokIdler);
    }

    public List<FaturaFis> getDosyadanAktarilanStokFisler(Sorgu sorgu) {
        sorgu.kriterEkle(KriterFactory.esit("veridenMi", true));
        return dao.sorgula(sorgu);
    }

    public void satisFisDosyaYukle(InputStream inputStream, Kullanici aktifKullanici) throws BusinessRuleException {
        Scanner scanner = new Scanner(inputStream);

        while (scanner.hasNextLine()) {
            satisFisSatirIsle(scanner.nextLine(), aktifKullanici);
        }

        scanner.close();
    }

    protected void satisFisSatirIsle(String aLine, Kullanici kullanici) throws BusinessRuleException {
        Scanner scanner = new Scanner(aLine);
        scanner.useDelimiter("&");
        if (scanner.hasNext()) {
            String muhatap = scanner.next();
            String faturaSeriNo = scanner.next();
            String faturaNo = scanner.next();
            String miktar = scanner.next();
            String miktar1 = scanner.next();
            String miktar2 = scanner.next();


            FirmaMusteri firmaMusteri = (FirmaMusteri) getDao().sorgula(new Sorgu(TuzelFirmaMusteri.class, null, KriterFactory.esit("ttkKurum.kurumNo", muhatap))).get(0);

            BolgeSatisFis bolgeSatisFis = new BolgeSatisFis();
            bolgeSatisFis.setFisTarihi(getSimdikiTarih());
            bolgeSatisFis.setMuhatap(firmaMusteri);
            bolgeSatisFis.setMuhatapTip(MuhatapTip.TESKILAT_ICI);
            bolgeSatisFis.setTerminAyi(Ay.NISAN);
            bolgeSatisFis.setTerminYili(2010);
            bolgeSatisFis.setBolgeFiyatTip(BolgeSatisFiyatTip.PESIN);
            bolgeSatisFis.setAciklama(miktar + "-" + miktar1 + "-" + miktar2 + " ADET YAZICI ÇIKIŞI");
            bolgeSatisFis.setKurum(kullanici.getAktifKurum());
            bolgeSatisFis.setVeridenMi(true);
            bolgeSatisFis.setFatura(new Fatura(faturaSeriNo, faturaNo, getSimdikiTarih()));
            satisFisiKes(bolgeSatisFis, kullanici, getSimdikiTarih());

            if (!miktar.equals("0")) {
                //todo:idleri ogren degistir..
                KurumStok birinciHareketKurumStok = yukle(KurumStok.class, 1158478);
                BolgeSatisFiyat birinciSatisFiyat = yukle(BolgeSatisFiyat.class, 1411068);
                BolgeSatisFisHareket birinciBolgeSatisFisHareket = new BolgeSatisFisHareket(birinciHareketKurumStok, new BigDecimal(miktar), birinciSatisFiyat, miktar + " ADET EPSON YAZICI");
                birinciBolgeSatisFisHareket.setKurum(kullanici.getAktifKurum());
                birinciBolgeSatisFisHareket.setKdvTutar(EkoopUtils.tutarCarp(new BigDecimal(miktar), new BigDecimal("100.80")));
                birinciBolgeSatisFisHareket.setMusterininOdeyecegiTutar(EkoopUtils.tutarCarp(new BigDecimal(miktar), new BigDecimal("660.80")));
                birinciBolgeSatisFisHareket.setOrtalamaMaliyet(new BigDecimal("535.24"));
                birinciBolgeSatisFisHareket.setTutar(EkoopUtils.tutarCarp(new BigDecimal(miktar), new BigDecimal("560")));
                birinciBolgeSatisFisHareket.setSatisFiyat(birinciSatisFiyat);
                bolgeSatisFis.hareketEkle(birinciBolgeSatisFisHareket);
            }

            if (!miktar1.equals("0")) {
                //todo:idleri ogren degistir..
                KurumStok ikinciHareketKurumStok = yukle(KurumStok.class, 1158198);
                BolgeSatisFiyat ikinciSatisFiyat = yukle(BolgeSatisFiyat.class, 1411070);

                BolgeSatisFisHareket ikinciBolgeSatisFisHareket = new BolgeSatisFisHareket(ikinciHareketKurumStok, new BigDecimal(miktar1), ikinciSatisFiyat, miktar1 + " ADET XEROX YAZICI");
                ikinciBolgeSatisFisHareket.setKurum(kullanici.getAktifKurum());
                ikinciBolgeSatisFisHareket.setKdvTutar(EkoopUtils.tutarCarp(new BigDecimal(miktar1), new BigDecimal("55.80")));
                ikinciBolgeSatisFisHareket.setMusterininOdeyecegiTutar(EkoopUtils.tutarCarp(new BigDecimal(miktar1), new BigDecimal("365.80")));
                ikinciBolgeSatisFisHareket.setOrtalamaMaliyet(new BigDecimal("303.12"));
                ikinciBolgeSatisFisHareket.setTutar(EkoopUtils.tutarCarp(new BigDecimal(miktar1), new BigDecimal("310")));
                ikinciBolgeSatisFisHareket.setSatisFiyat(ikinciSatisFiyat);
                bolgeSatisFis.hareketEkle(ikinciBolgeSatisFisHareket);
            }
            if (!miktar2.equals("0")) {
                //todo:idleri ogren degistir..
                KurumStok ikinciHareketKurumStok = yukle(KurumStok.class, 1158197);
                BolgeSatisFiyat ikinciSatisFiyat = yukle(BolgeSatisFiyat.class, 1411070);

                BolgeSatisFisHareket ikinciBolgeSatisFisHareket = new BolgeSatisFisHareket(ikinciHareketKurumStok, new BigDecimal(miktar2), ikinciSatisFiyat, miktar2 + " ADET XEROX YAZICI");
                ikinciBolgeSatisFisHareket.setKurum(kullanici.getAktifKurum());
                ikinciBolgeSatisFisHareket.setKdvTutar(EkoopUtils.tutarCarp(new BigDecimal(miktar2), new BigDecimal("55.80")));
                ikinciBolgeSatisFisHareket.setMusterininOdeyecegiTutar(EkoopUtils.tutarCarp(new BigDecimal(miktar2), new BigDecimal("365.80")));
                ikinciBolgeSatisFisHareket.setOrtalamaMaliyet(new BigDecimal("303.12"));
                ikinciBolgeSatisFisHareket.setTutar(EkoopUtils.tutarCarp(new BigDecimal(miktar2), new BigDecimal("310")));
                ikinciBolgeSatisFisHareket.setSatisFiyat(ikinciSatisFiyat);
                bolgeSatisFis.hareketEkle(ikinciBolgeSatisFisHareket);
            }


            fisOnayla(bolgeSatisFis);
        }

        scanner.close();
    }

    private SulamaSistemFisYoneticisi getSulamSistemFisYoneticisi() {
        return new SulamaSistemFisYoneticisi(this, getStokSayacYonetici(), muhasebeModulServis, dao, takvim, ortakTakipModulServis, ortakKrediModulServis);
    }

    public void sulamaSistemFisKes(SulamaSistemFis sulamaSistemFis, Kullanici kullanici) throws BusinessRuleException {
        getSulamSistemFisYoneticisi().sulamaSistemFisKes(sulamaSistemFis, kullanici);
    }


    public void sulamaSistemFisHareketEkle(SulamaSistemFis sulamaSistemFis, SulamaSistemFisHareket sulamaSistemFisHareket) throws BusinessRuleException {
        getSulamSistemFisYoneticisi().sulamaSistemFisHareketEkle(sulamaSistemFis, sulamaSistemFisHareket);
    }

//    public void sulamaSistemFisineMasrafEkle(SulamaSistemFis sulamaSistemFis, SulamaSistemMasraf sistemMasraf) throws BusinessRuleException {
//        getSulamSistemFisYoneticisi().sulamaSistemFisineMasrafEkle(sulamaSistemFis, sistemMasraf);
//    }
//
//    public void sulamaSistemFisindenMasrafSil(SulamaSistemFis sulamaSistemFis, SulamaSistemMasraf sulamaSistemMasraf) throws BusinessRuleException {
//        getSulamSistemFisYoneticisi().sulamaSistemFisindenMasrafSil(sulamaSistemFis, sulamaSistemMasraf);
//    }

    public void sulamaSistemFisOnayla(SulamaSistemFis sulamaSistemFis, EkHizmetBuro ekHizmetBuro) throws BusinessRuleException {
        getSulamSistemFisYoneticisi().sulamaSistemFisOnayla(sulamaSistemFis, ekHizmetBuro);
    }

    public StokIptalFis sulamaSistemFisIptal(SulamaSistemFis sulamaSistemFis, Kullanici kullanici, List<StokFis> kesilenFisler) throws BusinessRuleException {
        return getSulamSistemFisYoneticisi().sulamaSistemFisIptal(sulamaSistemFis, kullanici, kesilenFisler);
    }

    public List<SulamaSistemMalzeme> getSulamaMalzemeler(FaturaFis faturaFis) {
        Sorgu sorgu = new Sorgu(SulamaSistemMalzeme.class);
        sorgu.kriterEkle(KriterFactory.esit("stokFis", faturaFis));
        return dao.sorgula(sorgu);
    }

    public String gubreFisleriniMuhasebelestir(List<BolgeSatisFis> bolgeSatisFisler, Tarih mahsupFisTarih, Kullanici aktifKullanici) throws BusinessRuleException {
        return getSatisFisYonetici().gubreFisleriniMuhasebelestir(bolgeSatisFisler, mahsupFisTarih, aktifKullanici);
    }

    public void faturaFisSil(FaturaFis faturaFis) throws BusinessRuleException {
        getFaturaFisYoneticisi().faturaFisSil(faturaFis);
    }

    public void secilenleriTekMahsubaKes(List<StokFis> stokFisler, Tarih mahsupFisTarih, Kullanici aktifKullanici) throws BusinessRuleException {
        if (stokFisler.get(0) instanceof BolgeSatisFis) {
            getSatisFisYonetici().secilenleriTekMahsubaKes(stokFisler, mahsupFisTarih, aktifKullanici);
        }

        if (stokFisler.get(0) instanceof FaturaFis) {
            getFaturaFisYoneticisi().secilenleriTekMahsubaKes(stokFisler, mahsupFisTarih, aktifKullanici);
        }

    }

    public void faturaliMasrafFisKes(FaturaliMasrafFis faturaliMasrafFis, Kullanici kullanici) throws BusinessRuleException {
        getFaturaFisYoneticisi().faturaliMasrafFisKes(faturaliMasrafFis, kullanici);
    }

    public void faturaliMasrafFisHareketEkle(FaturaliMasrafFis faturaliMasrafFis, FaturaliMasrafFisHareket faturaliMasrafFisHareket, Kurum kurum) throws BusinessRuleException {
        getFaturaFisYoneticisi().faturaliMasrafFisHareketEkle(faturaliMasrafFis, faturaliMasrafFisHareket, kurum);
    }

    public void faturaliMasrafFisiOnayla(FaturaliMasrafFis faturaliMasrafFis) throws BusinessRuleException {
        getFaturaFisYoneticisi().faturaliMasrafFisiOnayla(faturaliMasrafFis);
    }

    public StokIptalFis faturaliMasrafFisIptal(FaturaliMasrafFis faturaliMasrafFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getFaturaFisYoneticisi().faturaliMasrafFisIptal(faturaliMasrafFis, kullanici, kesilenFisler);
    }

    public void kdvTevkifTutariHesapla(BolgeSatisFisHareket hareket) throws BusinessRuleException {
        getSatisFisYonetici().kdvTevkifTutarHesaplama(hareket);
    }

    public List<StokKoopBirlestirmeFis> stokKooperatifBirlestir(Kurum birlesenKurum, MahsupFis birlesenKoopMahsup, Kurum devralanKurum, MahsupFis devralanMahsup) throws BusinessRuleException {
        List<StokKoopBirlestirmeFis> fisListesi = new ArrayList<StokKoopBirlestirmeFis>();
        StokKoopBirlestirmeFis stokKoopBirlestirmeFis = kapanisKurumIslemiYap(birlesenKurum, birlesenKoopMahsup);
        if (stokKoopBirlestirmeFis == null) {
            return null;
        }
        fisListesi.add(stokKoopBirlestirmeFis);
        fisListesi.add(devralanKurumIslemiYap(stokKoopBirlestirmeFis, devralanKurum, devralanMahsup));
        return fisListesi;
    }

    private StokKoopBirlestirmeFis devralanKurumIslemiYap(StokKoopBirlestirmeFis kapanisStokFis, Kurum kurum, MahsupFis mahsupFis) throws BusinessRuleException {
        StokKoopBirlestirmeFis stokKoopBirlestirmeFis = new StokKoopBirlestirmeFis("GECICI000001",
                getSimdikiTarih(), "KOOPERATİF BİRLEŞTİRME DEVİR", StokFisKaynak.STOK, kurum,
                getSimdikiTarih(), mahsupFis.getKullanici());
        kaydet(stokKoopBirlestirmeFis);

        for (StokKoopBirlestirmeFisHareket hareket : kapanisStokFis.getHareketler()) {
            StokKoopBirlestirmeFisHareket devirHareket = new StokKoopBirlestirmeFisHareket(devralanKurumStokEslestirmesiYapYoksaAc(hareket.getStok(), kurum), hareket.getMiktar(), hareket.getTutar(), "STOK KOOP DEVİR FİŞİ ", StokHareketTip.GIRIS);
            stokKoopBirlestirmeFis.hareketEkle(devirHareket);

        }
        stokKoopBirlestirmeFis.setMuhasebeFisi(mahsupFis);
        fisOnayla(stokKoopBirlestirmeFis);
        return stokKoopBirlestirmeFis;
    }

    private KurumStok devralanKurumStokEslestirmesiYapYoksaAc(KurumStok kurumStok, Kurum kurum) throws BusinessRuleException {
        Stok stok = kurumStok.getStok();
        Sorgu sorgu = new Sorgu(KurumStok.class);
        sorgu.kriterEkle(KriterFactory.esit("stok", stok));
        List<KurumStok> kurumStoklari = getKurumStoklari(sorgu, kurum);
        if (kurumStoklari.size() > 0) {
            return kurumStoklari.get(0);
        } else {
            KurumStok yeniKurumStok = new KurumStok(stok, kurum, kurumStok.getAlisKdvOrani(), kurumStok.getSatisKdvOrani());
            yeniKurumStok.setAzamiStokMiktari(kurumStok.getAzamiStokMiktari());
            yeniKurumStok.setAsgariStokMiktari(kurumStok.getAsgariStokMiktari());
            yeniKurumStok.setKritikStokMiktari(kurumStok.getKritikStokMiktari());
            yeniKurumStok.setSatisValoru(kurumStok.getSatisValoru());
            yeniKurumStok.setAktif(true);
            yeniKurumStok.setTermineGoreMi(kurumStok.getTermineGoreMi());
            kaydet(yeniKurumStok);
            return yeniKurumStok;
        }

    }

    private StokKoopBirlestirmeFis kapanisKurumIslemiYap(Kurum kurum, MahsupFis mahsupFis) throws BusinessRuleException {
        StokKoopBirlestirmeFis stokKoopBirlestirmeFis = new StokKoopBirlestirmeFis("GECICI000001",
                getSimdikiTarih(), "KOOPERATİF BİRLEŞTİRME KAPANIŞ", StokFisKaynak.STOK, kurum,
                getSimdikiTarih(), mahsupFis.getKullanici());
        kaydet(stokKoopBirlestirmeFis);
        Map<KurumStok, List<BigDecimal>> miktarliStoklar = miktarliStoklariBul(kurum);

        for (KurumStok kurumStok : miktarliStoklar.keySet()) {
            List<BigDecimal> degerler = miktarliStoklar.get(kurumStok);
            BigDecimal mevcutMiktar = degerler.get(0);
            BigDecimal mevcutTutar = degerler.get(1);
            StokKoopBirlestirmeFisHareket hareket = new StokKoopBirlestirmeFisHareket(kurumStok, mevcutMiktar, mevcutTutar, "STOK KOOP KAPANIS DEVİR FİŞİ ", StokHareketTip.CIKIS);
            stokKoopBirlestirmeFis.hareketEkle(hareket);
        }

        if (miktarliStoklar.size() > 0) {
            stokKoopBirlestirmeFis.setMuhasebeFisi(mahsupFis);
            fisOnayla(stokKoopBirlestirmeFis);
            return stokKoopBirlestirmeFis;
        } else {
            sil(stokKoopBirlestirmeFis);
            return null;
        }


//bu sekildede olabilir.
//        Iterator<KurumStok> stoklar =  miktarliStoklar.keySet().iterator();
//        while(stoklar.hasNext()){
//
//            KurumStok kurumStok = stoklar.next();
//            List<BigDecimal> degerler =  miktarliStoklar.get(kurumStok);
//            BigDecimal mevcutMiktar= degerler.get(0);
//            BigDecimal mevcutTutar=degerler.get(1);
//
//        }


    }

    private Map miktarliStoklariBul(Kurum kurum) {

        Map<KurumStok, List<BigDecimal>> mevcutDurumMap = new HashMap<KurumStok, List<BigDecimal>>();

        List<KurumStok> tumStoklar = getKurumStokKartlari(kurum, new Sorgu(KurumStok.class));

        for (KurumStok kurumStok : tumStoklar) {
            List<BigDecimal> stokDegerler = new ArrayList<BigDecimal>();
            BigDecimal mevcutMiktar = getMevcutStokMiktari(kurumStok);
            BigDecimal mevcutTutar = getMevcutStokTutari(kurumStok);
            if (mevcutMiktar != null && mevcutMiktar.signum() != 0 && mevcutTutar != null && mevcutTutar.signum() != 0) {
                stokDegerler.add(mevcutMiktar);
                stokDegerler.add(mevcutTutar);
                mevcutDurumMap.put(kurumStok, stokDegerler);
            }
        }
        return mevcutDurumMap;
    }

    public List<FaturaFis> getFaturaFisler(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public void takipOzellikDegistir(StokFis stokFis, KurumStok kurumStok, String takipNo, Tarih takipTarih, Tarih sonKullanmaTarihi) throws BusinessRuleException {
        getFaturaFisYoneticisi().takipOzellikDegistir(stokFis, kurumStok, takipNo, takipTarih, sonKullanmaTarihi);
    }

    public SatisFiyatArttirimFis satisFiyatArtirimFisiOlustur(KurumStok kurumStok, BigDecimal miktar, BigDecimal farkBirimFiyat, Fatura fatura, FirmaMusteri muhatap, Kullanici kullanici, StokFisKaynak kaynak, String aciklama, Tarih tarih) throws BusinessRuleException {
        return getStokFisYonetici().satisFiyatArtirimFisiOlustur(kurumStok, miktar, farkBirimFiyat, fatura, muhatap, kullanici, kaynak, aciklama, tarih);

    }

    public StokKartDuzeltmeFisi stokKartDuzeltmeFisiKes(KurumStok kurumStok, BigDecimal miktar, BigDecimal tutar, StokHareketTip tip, Kullanici kullanici, String aciklama, boolean oncekiYilaKesilecek, boolean oncekiAyaKesilecek, EkHizmetBuro ekHizmetBuro, String takipNo, Tarih takipTarih, Tarih sonKullanmaTarihi) throws BusinessRuleException {
        return getStokFisYonetici().stokKartDuzeltmeFisiKes(kurumStok, miktar, tutar, tip, kullanici, aciklama, oncekiYilaKesilecek, oncekiAyaKesilecek, ekHizmetBuro, takipNo, takipTarih, sonKullanmaTarihi);

    }

    public StokFis stokAktarmaFisiKes(KurumStok aktarilanStok, KurumStok aktarilacakStok, BigDecimal miktar, BigDecimal tutar, Kullanici kullanici, String aciklama) throws BusinessRuleException {
        return getStokFisYonetici().stokAktarmaFisiKes(aktarilanStok, aktarilacakStok, miktar, tutar, kullanici, aciklama);
    }

    public List<KurumStok> getKurumStokKartlariKrediModulu(Sorgu sorgu, Kurum kurum, long krediId) {
        return dao.getKurumStokKartlariKrediModulu(sorgu, kurum, krediId);
    }

    public BigDecimal getMevcutStokMiktariTakipOzellikli(KurumStok kurumStok, TakipOzelligi takipOzelligi) {
        return getMevcutStokMiktari(kurumStok, takipOzelligi);
    }

    public List<TakipOzelligi> getTakipOzellikler(KurumStok kurumStok) {
        return dao.getTakipOzellikler(kurumStok);
    }

    public List<StokBilgi> stokYilbasiKontrol(Kurum kurum, boolean yilIciKontrolMu) {
        kurum = yukle(Kurum.class, kurum.getId());
// System.out.println("Kurum Stok Kartları çekilecek " + kurum.getAd());
        List<KurumStok> tumStoklar = new ArrayList<KurumStok>();
        if (yilIciKontrolMu == false) {
            tumStoklar = getKurumStokKartlari(kurum, new Sorgu(KurumStok.class));
        } else {
            tumStoklar = getYilIcindeIslemGorenKurumStokKartlari(kurum, getSimdikiTarih().getYil());
        }

// System.out.println("Kurum Stok Kartları çekildiii, size : " + tumStoklar.size());
        List<StokBilgi> stokBilgiler = new ArrayList<StokBilgi>();
        for (KurumStok kurumStok : tumStoklar) {
            flush();


//            System.out.println("Stoka basladim-----" + kurumStok.getStok().getStokKodu() + "----" + new Tarih());
            StokBilgi stokBilgi = new StokBilgi(kurumStok, getMevcutStokMiktari(kurumStok), getMevcutStokTutari(kurumStok));
            if (yilIciKontrolMu == true) {

                if (stokBilgi.getHatali().equals("Aktarilacak")) {
                    stokBilgi.setHatali("ISLEMDE");
                }

                if (!stokBilgi.getHatali().equals("Aktarilmayacak")) {
                    stokBilgiler.add(stokBilgi);
                }


            } else {
                stokBilgiler.add(stokBilgi);
            }

//            System.out.println("Stogu bitirdim****" + kurumStok.getStok().getStokKodu() + "*****" + new Tarih());
        }

//  System.out.println("++++++++++++++++++Döngü bitti, sort edilecek");
        Collections.sort(stokBilgiler, new Comparator<StokBilgi>() {
            public int compare(StokBilgi sb1, StokBilgi sb2) {
                return sb1.getHatali().compareTo(sb2.getHatali());
//                return sb1.getKurumStok().getStok().getStokKodu().compareTo(sb2.getKurumStok().getStok().getStokKodu());
            }
        });

        return stokBilgiler;
    }

    public List<StokBilgi> konsinyeYilbasiKontrol(Kurum kurum) {
        kurum = yukle(Kurum.class, kurum.getId());
        List<KurumStok> tumKonsinyeStoklar = getYilIcindeIslemGorenKurumKonsinyeKartlari(kurum);
        List<StokBilgi> stokBilgiler = new ArrayList<StokBilgi>();
        for (KurumStok kurumStok : tumKonsinyeStoklar) {
            //            System.out.println("Bu Stoga basladim-----" + kurumStok.getStok().getStokKodu() + "----" + new Tarih());
            StokBilgi stokBilgi = new StokBilgi(kurumStok, getMevcutKonsinyeStokMiktari(kurumStok), "KONSİNYE");
            stokBilgiler.add(stokBilgi);
            //            System.out.println("Bu Stoga bitirdim****" + kurumStok.getStok().getStokKodu() + "*****" + new Tarih());
        }
        Collections.sort(stokBilgiler, new Comparator<StokBilgi>() {
            public int compare(StokBilgi sb1, StokBilgi sb2) {
                return sb1.getHatali().compareTo(sb2.getHatali());
                //                return sb1.getKurumStok().getStok().getStokKodu().compareTo(sb2.getKurumStok().getStok().getStokKodu());
            }
        });

        // System.out.println("********** Sort bittti");
        return stokBilgiler;
    }


    public StokYilbasiFis yilbasiDevirFisiKes(List<StokBilgi> stokBilgileri, Kullanici kullanici) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(kullanici.getAktifKurum(), getSimdikiTarih().getYil() + "_YilbasiFisiKes", "", true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }

        return getStokFisYonetici().yilbasiDevirFisiKes(stokBilgileri, kullanici);

    }

    public KonsinyeGirisFis yilbasiKonsinyeGirisFisiKes(List<StokBilgi> konsinyeBilgileri, Kullanici kullanici) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(kullanici.getAktifKurum(), getSimdikiTarih().getYil() + "_YilbasiKonsinyeFisiKes", "", true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }

        return getStokFisYonetici().yilbasiKonsinyeGirisFisiKes(konsinyeBilgileri, kullanici);

    }


    public void stokExcelAktar(List<StokBilgi> stokBilgileri, Kullanici kullanici) throws IOException {
        String filename = "tmp\\stok_kontrol_" + kullanici.getAktifKurum().getKurumNo() + "_" + kullanici.getKullaniciAdi() + ".xls";

        WorkbookSettings ws = new WorkbookSettings();
        ws.setLocale(new Locale("tr", "TR"));
        WritableWorkbook workbook =
                Workbook.createWorkbook(new File(filename), ws);
        WritableSheet s = workbook.createSheet("Mugurlu", 0);

        try {
            writeDataSheet(s, stokBilgileri);
            workbook.write();
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void writeDataSheet(WritableSheet s, List<StokBilgi> stokBilgileri) throws WriteException {
        WritableFont wf = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
        WritableCellFormat cf = new WritableCellFormat(wf);
        cf.setWrap(true);
        int satirNo = 1;
        Label stokKodu = new Label(0, 0, "Stok Kodu", cf);
        s.addCell(stokKodu);
        Label miktar = new Label(1, 0, "Miktar", cf);
        s.addCell(miktar);
        Label tutar = new Label(2, 0, "Tutar", cf);
        s.addCell(tutar);
        Label aktarma = new Label(3, 0, "Aktarma Durumu", cf);
        s.addCell(aktarma);
        Label hesap = new Label(4, 0, "Koop No", cf);
        s.addCell(hesap);
//        Label hesapTutar = new Label(5, 0, "Hesap Tutari", cf);
//        s.addCell(hesapTutar);

        for (int i = 0; i < stokBilgileri.size(); ++i) {
            StokBilgi stokBilgi = stokBilgileri.get(i);
            Label stokKoduDeger = new Label(0, satirNo + i, stokBilgi.getKurumStok().getStok().getStokKodu(), cf);
            s.addCell(stokKoduDeger);

            Label stokMiktar = new Label(1, satirNo + i, stokBilgi.getMiktar().toString(), cf);
            s.addCell(stokMiktar);

            Label stokTutar = new Label(2, satirNo + i, stokBilgi.getTutar().toString(), cf);
            s.addCell(stokTutar);

            Label stkAktar = new Label(3, satirNo + i, stokBilgi.getHatali(), cf);
            s.addCell(stkAktar);

            Label stkKoop = new Label(4, satirNo + i, stokBilgi.getKurumStok().getKurum().getNo(), cf);
            s.addCell(stkKoop);

//            if (stokBilgi.getHatali().equals("Aktarilacak")) {
//                Label stokHesap = new Label(4, satirNo + i, stokBilgi.getMuhasebeHesapNo(), cf);
//                s.addCell(stokHesap);
//
//                Label stokHesapTutar = new Label(5, satirNo + i, stokBilgi.getHesapBakiye().toString(), cf);
//                s.addCell(stokHesapTutar);
//
//            }


        }


    }

    public List<BolgeSatisFiyat> getMerkezSatisFiyatlar(KurumStok kurumStok) {
        Sorgu sorgu = new Sorgu(BolgeSatisFiyat.class);
        sorgu.kriterEkle(KriterFactory.esit("kurumStok", kurumStok));
        Tarih tarih = getSimdikiTarih().ayCikar(6);
        sorgu.kriterEkle(KriterFactory.buyuk("gecerliTarih", tarih));

        return dao.sorgula(sorgu);
    }


    public void flush() {
        commonServis.flush();
    }

    public boolean yilbasiDevirFisiKontrol(Kurum kurum) {
        return dao.stokYilbasiDevirFisiVarMi(kurum);
    }


    public BolgeSatisFiyat getSatisFiyatMerkezForSevk(KurumStok kurumStok, Tarih tarih, BolgeSatisFiyatTip fiyatTipi, BigDecimal satisFiyati) {
        return dao.getSatisFiyatMerkezForSevk(kurumStok, tarih, fiyatTipi, satisFiyati);
    }

    public String gubreFisleriniOnOdemeliMuhasebelestir(List<BolgeSatisFis> bolgeSatisFisler, Tarih mahsupFisTarih, Kullanici kullanici) throws BusinessRuleException {
        return getSatisFisYonetici().gubreFisleriniOnOdemeliMuhasebelestir(bolgeSatisFisler, mahsupFisTarih, kullanici);
    }

    public KoopSatisFiyat krediliKoopSatisFiyatEkle(KurumStok kurumStok, Tarih tarih, BigDecimal satisFiyati) throws BusinessRuleException {
        Sorgu sorgu = new Sorgu(KoopSatisFiyat.class);
        sorgu.kriterEkle(KriterFactory.esit("kurumStok", kurumStok));
        sorgu.kriterEkle(KriterFactory.esit("fiyat", satisFiyati));
        sorgu.kriterEkle(KriterFactory.or(KriterFactory.esit("fiyatTipi", KoopSatisFiyatTip.ILERI_KREDILI), KriterFactory.esit("fiyatTipi", KoopSatisFiyatTip.KREDILI)));
        List<Object> satisFiyatlar = dao.sorgula(sorgu);
        if (satisFiyatlar.size() > 0) {
            return (KoopSatisFiyat) satisFiyatlar.get(0);
        }

        KoopSatisFiyat satisFiyat = new KoopSatisFiyat(kurumStok, satisFiyati);
        satisFiyat.setFiyatTipi(KoopSatisFiyatTip.ILERI_KREDILI);
        satisFiyat.setMuhatapTipi(MuhatapTip.ORTAK_ICI);
        satisFiyat.setGecerliTarih(tarih);
        kaydet(satisFiyat);
        return satisFiyat;
    }

    public List<SatisFiyatArttirimFis> getSatisFiyatArtirimFisler(Sorgu sorgu, Kurum kurum) {
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        return dao.sorgula(sorgu);
    }

    public void gubreSatisFiyatArtirimFisleriniMuhasebelestir(List<SatisFiyatArttirimFis> fisler, Tarih mahsupFisTarih, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().gubreSatisFiyatArtirimFisleriniMuhasebelestir(fisler, mahsupFisTarih, kullanici);
    }

    public List<KurumStok> getYilIcindeIslemGorenKurumStokKartlari(Kurum kurum, int yil) {
        return dao.getYilIcindeIslemGorenKurumStokKartlari(kurum, yil);
    }

    public List<KurumStok> getYilIcindeIslemGorenKurumKonsinyeKartlari(Kurum kurum) {
        return dao.getYilIcindeIslemGorenKurumKonsinyeKartlari(kurum);
    }

    public void stokYasaklamaKaydet(StokYasaklamaKurali stokYasaklamaKurali) throws BusinessRuleException {
        if (stokYasaklamaKurali.getDurdurmaKurumTuru().equals(DurdurmaKurumTuru.BOLGE_VE_BOLGEYE_BAGLI_KOOPERATIFLERE)) {
            if (!stokYasaklamaKurali.getKurum().isBolge()) {
                throw new BusinessRuleException(StokHataKodu.BOLGE_SECMELISINIZ);
            }
        }

        if (stokYasaklamaKurali.getDurdurmaKurumTuru().equals(DurdurmaKurumTuru.SADECE_BU_KURUMA)) {
            if (stokYasaklamaKurali.getKurum() == null) {
                throw new BusinessRuleException(StokHataKodu.KURUM_SECMELISINIZ);
            }
        }

        if (stokYasaklamaKurali.getDurdurmaKurumTuru().equals(DurdurmaKurumTuru.TUM_BOLGELERE) || stokYasaklamaKurali.getDurdurmaKurumTuru().equals(DurdurmaKurumTuru.TUM_KURUMLARA)) {
            stokYasaklamaKurali.setKurum(null);

        }

        if (stokYasaklamaKurali.getDurdurmaKurumTuru().equals(DurdurmaKurumTuru.TUM_KURUMLARA) && stokYasaklamaKurali.getStokDurdurmaTuru().equals(StokDurdurmaTuru.TUM)) {
            stokKartiniKullanimaKapat(stokYasaklamaKurali);
        }
        kaydet(stokYasaklamaKurali);


    }

    private void stokKartiniKullanimaKapat(StokYasaklamaKurali stokYasaklamaKurali) {
        List<Stok> stoklar;
        KartDurumu kartDurumu;
        Sorgu sorgu = new Sorgu(Stok.class);
        if (stokYasaklamaKurali.getKaldirmaTarihi() == null) {
            kartDurumu = KartDurumu.YASAKLI;
        } else {
            kartDurumu = KartDurumu.KULLANIMDA;
        }


        if (stokYasaklamaKurali.getStok() != null) {
            stokYasaklamaKurali.getStok().setKartDurumu(kartDurumu);

        }

        if (stokYasaklamaKurali.getEtkinMadde() != null) {
            sorgu.kriterEkle(KriterFactory.esit("etkinMaddeOrani.etkinMadde", stokYasaklamaKurali.getEtkinMadde()));
            stoklar = getStoklar(sorgu);
            for (Stok stok : stoklar) {
                stok.setKartDurumu(kartDurumu);
            }
        }

        if (stokYasaklamaKurali.getFirma() != null) {
            sorgu.kriterEkle(KriterFactory.esit("firma", stokYasaklamaKurali.getFirma()));
            stoklar = getStoklar(sorgu);
            for (Stok stok : stoklar) {
                stok.setKartDurumu(kartDurumu);
            }
        }

        if (stokYasaklamaKurali.getStokGrup() != null) {
            sorgu.kriterEkle(KriterFactory.esit("grup", stokYasaklamaKurali.getStokGrup()));
            stoklar = getStoklar(sorgu);
            for (Stok stok : stoklar) {
                stok.setKartDurumu(kartDurumu);
            }
        }


    }

    public void validateKullanimdaKartMi(KurumStok kurumStok) throws BusinessRuleException {
        if (!kurumStok.isKullanimda()) {
            throw new BusinessRuleException(StokHataKodu.MERKEZ_BIRLIGI_BU_STOKU_KULALNIMINA_KAPATMISTIR);
        }

    }

    public void konsinyeKartDüzelt(StokFis fis, KurumStok kurumStok, BigDecimal miktar) throws BusinessRuleException {
        List<StokHareket> hareketler = fis.getHareketler();
        for (StokHareket hareket : hareketler) {
            if (hareket.getStok().equals(kurumStok)) {
                hareket.setMiktar(miktar);
                kaydet(hareket);
            }
        }
    }


    public BigDecimal getStokKebir150Ve153MuhasebeHesapBakiye(Kurum kurum, Tarih simdikiTarih) throws BusinessRuleException {
        Hesap hesap150 = muhasebeModulServis.getTumKurumlaraAitAcikHesapByNo("150");
        Hesap hesap153 = muhasebeModulServis.getTumKurumlaraAitAcikHesapByNo("153");
        BigDecimal hesap150Bakiye = muhasebeModulServis.getKebirHesapBakiyesiTariheKadar(kurum, hesap150, simdikiTarih).getBorcBakiye();
        BigDecimal hesap153Bakiye = muhasebeModulServis.getKebirHesapBakiyesiTariheKadar(kurum, hesap153, simdikiTarih).getBorcBakiye();
        return hesap150Bakiye.add(hesap153Bakiye);
    }

    public BitkiKorumaPersonel getRecetePersonel(Kullanici kullanici) throws BusinessRuleException {
        Sorgu sorgu = new Sorgu(BitkiKorumaPersonel.class);
        sorgu.kriterEkle(KriterFactory.esit("personel", kullanici.getPersonel()));
        if (dao.sorgula(sorgu).size() == 0) {
            throw new BusinessRuleException(StokHataKodu.RECETE_DUZENLEME_YETKISI_OLMAYAN_BU_ISLEMI_DUZENLEYEMEZ);
        }
        return (BitkiKorumaPersonel) dao.sorgula(sorgu).get(0);
    }

    public void kullaniciyaMailAt(Kullanici kullanici, String mesaj) throws BusinessRuleException {
        mailer.sendMail("ttkmailenableduser@tarimkredi.org.tr", "Stok Modülü Tohumluk alis KDV uyari ", mesaj,
                new String[]{kullanici.getPersonel().getOzlukBilgileri().getIrtibatBilgisi().getEmailAdresi()});


    }


    public BigDecimal getEkHizmetBuroMevcutStokMiktari(KurumStok kurumStok, TakipOzelligi takipOzelligi, EkHizmetBuro ekHizmetBuro, Tarih fisTarihi) {
        return bakiyeHesaplayici.getEkHizmetBuroMevcutStokMiktari(kurumStok, takipOzelligi, ekHizmetBuro, fisTarihi);
    }


    public StokFis depoAktarimFisiKes(KurumStok kurumStok, BigDecimal miktar, BigDecimal tutar, EkHizmetBuro cikisBuro, EkHizmetBuro girisBuro, TakipOzelligi takipOzellik, String aciklama, Kullanici kullanici) throws BusinessRuleException {
        return getStokFisYonetici().depoAktarimFisiKes(kurumStok, miktar, tutar, cikisBuro, girisBuro, takipOzellik, aciklama, kullanici);
    }


    public FaturaFis kurumGeciciFisOlustur(SevkStokCikisFis sevkStokCikisFis, Kullanici kullanici) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(kullanici.getAktifKurum(), "SevkGeciciFisOlusturma", sevkStokCikisFis.getId() + "", true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }
        return getFaturaFisYoneticisi().kurumGeciciFisOlustur(sevkStokCikisFis, kullanici);
    }


    public FaturaFis kurumlarArasiGeciciFaturaFisOlustur(StokFis stokFis, Kullanici kullanici) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(kullanici.getAktifKurum(), "KRMArasiGeciciFisOlusturma", stokFis.getId() + "", true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }

        return getFaturaFisYoneticisi().kurumlarArasiGeciciFaturaFisOlustur(stokFis, kullanici);
    }


    public void stokYasakGir(Kullanici kullanici) throws BusinessRuleException {
        Sorgu sorgu = new Sorgu(Stok.class);
        FirmaMusteri firma = yukle(FirmaMusteri.class, 1130);
        sorgu.kriterEkle(KriterFactory.esit("firma", firma));
        List<Stok> stoklar = sorgula(sorgu);
        for (Stok stok : stoklar) {
            StokYasaklamaKurali sto = new StokYasaklamaKurali();
            sto.setStokDurdurmaTuru(StokDurdurmaTuru.TUM);
            sto.setDurdurmaKurumTuru(DurdurmaKurumTuru.TUM_KURUMLARA);
            sto.setStok(stok);
            sto.setBaslangicTarihi(new Tarih());
            sto.setAciklama("FIRMANIN BU ÜRÜNLER İLE İLGİLİ SÖZLEŞMESİ SONA ERMİŞTİR.YENI AÇILAN STOK KARTLARINI KULLANINIZ");
            kaydet(sto);
        }

    }


    public BigDecimal getFirmaninYaptigiKredisizKdvliPesinSatislar(FirmaMusteri muhatap, Tarih baslangicTarihi, Tarih bitisTarihi) {
        return dao.getFirmaninYaptigiKredisizKdvliPesinSatislar(muhatap, baslangicTarihi, bitisTarihi);
    }

    public BigDecimal getMevcutStokMiktariKulakKupeNumarali(KurumStok kurumStok, String kulakKupeNo, EkHizmetBuro ekHizmetBuro, Tarih fisTarihi) {
        return bakiyeHesaplayici.getMevcutStokMiktariKulakKupeNumarali(kurumStok, kulakKupeNo, ekHizmetBuro, fisTarihi);
    }


    public boolean validateKulakKupeOncedenGirilmisMi(StokFis fis, StokHareket hareket) {
        return dao.validateKulakKupeOncedenGirilmisMi(fis, hareket);
    }


    public void stokDevirHızMiktarlariOlustur(Kullanici kullanici, int yil, Tarih veriTarihi, Kurum bolge, Kurum kurum) throws BusinessRuleException {
        Sorgu sorgu = new Sorgu(KurumStok.class);
        if (kurum == null) {
            sorgu.kriterEkle(KriterFactory.esit("kurum.ustKurum", bolge));
        } else {
            sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        }
        List<KurumStok> kurumStoklar = sorgula(sorgu);

        for (KurumStok kurumStok : kurumStoklar) {
            flush();
            StokMiktarDevirHizi devirHizi = new StokMiktarDevirHizi(kullanici, yil, veriTarihi, kurumStok, kurumStok.getStok(), stokDevirHizMiktariHesapla(kurumStok, yil));
            kaydet(devirHizi);
        }


    }

    public BigDecimal stokDevirHizMiktariHesapla(KurumStok kurumStok, int yil) {
        BigDecimal sonuc = BigDecimal.ZERO;
        BigDecimal ortalamaEnvanter = BigDecimal.ZERO;
        BigDecimal yilbasiDevirMiktar = BigDecimal.ZERO;
        BigDecimal yilSonuDevirMiktar = BigDecimal.ZERO;
        BigDecimal yilToplamCikanMiktar = BigDecimal.ZERO;
        if (yil < 2010) {
            return BigDecimal.ZERO;
        } else if (yil > getSimdikiTarih().getYil()) {
            return BigDecimal.ZERO;
        } else {
            yilbasiDevirMiktar = yilbasiDevirFisindekiMiktariBul(kurumStok, yil);
            yilToplamCikanMiktar = getYilToplamCikanStokMiktar(kurumStok, yil) == null ? BigDecimal.ZERO : getYilToplamCikanStokMiktar(kurumStok, yil);
            if (yil < getSimdikiTarih().getYil()) {
                yil = yil + 1;
            }
            yilSonuDevirMiktar = yilbasiDevirFisindekiMiktariBul(kurumStok, yil);

        }

        ortalamaEnvanter = (yilbasiDevirMiktar.add(yilSonuDevirMiktar)).divide(new BigDecimal(2));
        if (ortalamaEnvanter == BigDecimal.ZERO) {
            //Verimli Stok Yönetimi demek icin asagidaki kodu kullandik.
            return new BigDecimal("5555");
        }

        if (EkoopUtils.isBuyuk(ortalamaEnvanter, BigDecimal.ZERO) && yilToplamCikanMiktar == BigDecimal.ZERO) {
            //Verimsiz Stok Yönetimi demek icin asagidaki kodu kullandik.
            return new BigDecimal("6666");
        }
        sonuc = EkoopUtils.bolVeYuvarla(yilToplamCikanMiktar, ortalamaEnvanter, 2);
        return sonuc;
    }

    private BigDecimal getYilToplamCikanStokMiktar(KurumStok kurumStok, int yil) {
        return dao.getYilToplamCikanStokMiktar(kurumStok, yil);
    }

    private BigDecimal yilbasiDevirFisindekiMiktariBul(KurumStok kurumStok, int yil) {
        BigDecimal sonuc = BigDecimal.ZERO;
        StokYilbasiFis yilbasiFis = null;
        Sorgu sorgu = new Sorgu(StokYilbasiFis.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurumStok.getKurum()));
        sorgu.kriterEkle(KriterFactory.esit("yil", yil));

        if (sorgula(sorgu).size() > 0) {
            yilbasiFis = (StokYilbasiFis) sorgula(sorgu).get(0);

            for (StokYilbasiFisHareket hareket : yilbasiFis.getHareketler()) {
                flush();
                if (kurumStok == hareket.getStok()) {
                    sonuc = hareket.getMiktar();
                }
            }
        }
        return sonuc;

    }

    public StokFis stokKusuratTasfiyesiYap(KurumStok kurumStok, BigDecimal mevcutMiktar, BigDecimal mevcutTutar, Kullanici aktifKullanici, boolean muhasebeliMi, boolean oncekiAyaKesilecek) throws BusinessRuleException {
        return getStokFisYonetici().stokKusuratTasfiyesiYap(kurumStok, mevcutMiktar, mevcutTutar, aktifKullanici, muhasebeliMi, oncekiAyaKesilecek);
    }


    public StokFis stokAktarmasiYap(KurumStok kurumStok, BigDecimal miktar, BigDecimal tutar, Stok aktarmaYapilacakStok, Kullanici kullanici, Boolean parcaliMi, BigDecimal aktMiktar, BigDecimal aktTutar) throws BusinessRuleException {
        return getStokFisYonetici().stokAktarmasiYap(kurumStok, miktar, tutar, aktarmaYapilacakStok, kullanici, parcaliMi, aktMiktar, aktTutar);
    }

    public FtpSunucuYoneticisi getFtpSunucuYoneticisi() {
        return commonServis.getFtpSunucuYoneticisi();
    }

    public FtpReadYetki getFtpReadYetki() {
        return commonServis.getFtpReadYetki();
    }


    public void firmaBelgeKaydet(StokFirmaBelge stokFirmaBelge, UploadedFile uploadedFile) throws BusinessRuleException {
        stokFirmaBelge.setIslemTarihi(getSimdikiTarih());
        stokFirmaBelge.setYil(getSimdikiTarih().getYil());
        if (uploadedFile != null) {
            String uploadedFileName = uploadedFile.getName();
            int yil = stokFirmaBelge.getYil();
            String filename = "";
            List<Integer> nokta = new ArrayList<Integer>();
            List<String> directory = new ArrayList<String>();
            directory.add("Evrak");
            directory.add("Stok");
            directory.add("FirmaBelge");
            directory.add(Integer.toString(yil));

            for (int i = 0; i < uploadedFileName.length(); i++) {
                if ('.' == uploadedFileName.charAt(i)) {
                    nokta.add(i);
                }
            }

            int index = nokta.get(nokta.size() - 1);
            String fileExtension = uploadedFileName.substring(index, uploadedFileName.length());

            if (uploadedFile.getSize() > 20971520) {
                throw new BusinessRuleException(CommonHataKodu.DOSYA_COK_BUYUK);
            }

            commonServis.fileTypeKontrol(fileExtension, uploadedFile);
            filename = stokFirmaBelge.getFirmaMusteri().getId() + "-" + stokFirmaBelge.getBelgeNo() + "-" + stokFirmaBelge.getFirmaDosyaNo() + fileExtension;
            getFtpSunucuYoneticisi().dosyaYukle(uploadedFile, filename, directory);
            stokFirmaBelge.setDosyaAdi(filename);

        }
//        firmaEmailGonder(stokFirmaBelge.getFirmaMusteri());
        kaydet(stokFirmaBelge);
    }


    private void firmaEmailGonder(TuzelFirmaMusteri firmaMusteri) throws BusinessRuleException {
        String emailMesaji = "Girmiş olduğunuz Belge sitemimize kaydedilmiştir";
        String emailKonu = "Firma Belge İşlemleri---";
        mailer.sendMail("ttkmailenableduser@tarimkredi.org.tr", emailKonu, emailMesaji,
                new String[]{firmaMusteri.getEmail()});
    }


    public KurumStok getKurumStokKarti(Stok stok, Kurum kurum) {
        Sorgu sorgu = new Sorgu(KurumStok.class);
        sorgu.kriterEkle(KriterFactory.esit("stok", stok));
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        if (sorgula(sorgu).size() > 0) {
            return (KurumStok) sorgula(sorgu).get(0);
        }
        return null;
    }

    public Stok getStokKarti(String stokKodu) {
        Sorgu sorgu = new Sorgu(Stok.class);
        sorgu.kriterEkle(KriterFactory.esit("stokKodu", stokKodu));
        if (sorgula(sorgu).size() > 0) {
            return (Stok) sorgula(sorgu).get(0);
        }
        return null;
    }


    public void devletDestekliSatisFisleriniMuhasebelestir(List<BolgeSatisFis> bolgeSatisFisler, Tarih mahsupFisTarih, Kullanici aktifKullanici) throws BusinessRuleException {
        getSatisFisYonetici().devletDestekliSatisFisleriniMuhasebelestir(bolgeSatisFisler, mahsupFisTarih, aktifKullanici);

    }

    public void devletDestekliSatisFisleriniTekTekMuhasebelestir(List<BolgeSatisFis> bolgeSatisFisler, Tarih mahsupFisTarih, Kullanici aktifKullanici) throws BusinessRuleException {
        getSatisFisYonetici().devletDestekliSatisFisleriniTekTekMuhasebelestir(bolgeSatisFisler, mahsupFisTarih, aktifKullanici);
    }

    public String getBarkodlaStokAdi(String barkod) {
        Sorgu sorgu = new Sorgu(Stok.class);
        sorgu.kriterEkle(KriterFactory.esit("barkod", barkod));
        sorgu.kriterEkle(KriterFactory.esit("kartDurumu", KartDurumu.KULLANIMDA));
        if (sorgula(sorgu).size() > 0) {
            return ((Stok) sorgula(sorgu).get(0)).getStokAdi();

        }

        return "--";
    }

    public void barkodluSatisIslemleriHesapla(List<BarkodIslem> barkodIslemler, StokFis stokFis) throws BusinessRuleException {
        MuhatapTip muhatapTip = ((KoopSatisFis) stokFis).getMuhatapTip();
        for (BarkodIslem barkodIslem : barkodIslemler) {
            KurumStok kurumStok = getKurumStokKartiBarkodla(barkodIslem.getBarkod(), stokFis.getKurum());
            barkodIslem.setKurumStok(kurumStok);
            if (kurumStok.isTakipOzelligiYok() && EkoopUtils.isBuyuk(barkodIslem.getMiktar(), getMevcutStokMiktari(kurumStok))) {
                throw new BusinessRuleException(StokHataKodu.STOKTAKI_MEVCUT_MIKTAR_YETERLI_DEGIL);
            }

            if (kurumStok.isSarjTakipOzellikli() && EkoopUtils.isBuyuk(barkodIslem.getMiktar(), getMevcutStokMiktari(kurumStok, barkodIslem.getTakipOzelligi()))) {
                throw new BusinessRuleException(StokHataKodu.STOKTAKI_MEVCUT_MIKTAR_YETERLI_DEGIL);
            }

            barkodIslem.setBirimFiyat(getSatisFiyatKoop(kurumStok, stokFis.getFisTarihi(), muhatapTip).getFiyat());
            barkodIslem.setToplamTutar(EkoopUtils.tutarCarp(barkodIslem.getMiktar(), barkodIslem.getBirimFiyat()));
            BigDecimal kdvsizDeger = EkoopUtils.bolVeYuvarla(barkodIslem.getToplamTutar(), kurumStok.getStok().getSatisKdvOrani().getOran().add(BigDecimal.ONE), 2);
            barkodIslem.setKdvsizTutar(kdvsizDeger);
            barkodIslem.setKdvTutar(barkodIslem.getToplamTutar().subtract(kdvsizDeger));
            BigDecimal iskontoDeger = BigDecimal.ZERO;
            BigDecimal iskontoTutar = BigDecimal.ZERO;
            if (kurumStok.getSatisIskontoOran() != null) {
                iskontoDeger = kurumStok.iskontoHesapla(barkodIslem.getBirimFiyat());
                iskontoTutar = (EkoopUtils.tutarCarp(kdvsizDeger, iskontoDeger));
            }
            if (kurumStok.getSatisIskontoTutari() != null) {
                iskontoDeger = kurumStok.iskontoHesapla(barkodIslem.getBirimFiyat());
                iskontoTutar = (EkoopUtils.tutarCarp(iskontoDeger, barkodIslem.getMiktar()));
            }

            if (EkoopUtils.isBuyuk(iskontoTutar, BigDecimal.ZERO)) {
                barkodIslem.setIskontoTutar(iskontoTutar);
            }
            BigDecimal mot = EkoopUtils.tutarYuvarla(barkodIslem.getKdvsizTutar().add(barkodIslem.getKdvTutar().subtract(iskontoTutar)));
            barkodIslem.setMot(mot);
            kaydet(barkodIslem);

        }

    }

    public KurumStok getKurumStokKartiBarkodla(String barkod, Kurum kurum) throws BusinessRuleException {
        Sorgu sorgu = new Sorgu(KurumStok.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        sorgu.kriterEkle(KriterFactory.esit("stok.barkod", barkod));
        if (sorgula(sorgu).size() == 0) {
            throw new BusinessRuleException(StokHataKodu.STOK_KARTINI_TANIMLAMANIZ_GEREKIR);
        }
        return (KurumStok) sorgula(sorgu).get(0);
    }

    public void barkodluIslemleriKoopSatisFisieneEkle(KoopSatisFis stokFis, List<BarkodIslem> barkodluUrunler) throws BusinessRuleException {
        //burayı konuş yap
        for (BarkodIslem barkodIslem : barkodluUrunler) {
            KoopSatisFisHareket hareket = new KoopSatisFisHareket();
            hareket.setStok(barkodIslem.getKurumStok());
            hareket.setTakipOzelligi(barkodIslem.getTakipOzelligi());
            hareket.setAlacakTutar(barkodIslem.getMot());
            hareket.setTutar(barkodIslem.getKdvsizTutar());
            hareket.setMusterininOdeyecegiTutar(barkodIslem.getMot());
            hareket.setRaporIndirimTutar(barkodIslem.getIskontoTutar());
            hareket.setKdvTutar(barkodIslem.getKdvTutar());
            stokFis.hareketEkle(hareket);
            kaydet(stokFis);
        }
    }


    public void denemeIslemYap(Stok stok) throws BusinessRuleException {


        Stok s = yukle(Stok.class, stok.getId());

        String data = s.getStokKodu();
//        String data = "Kimyevi gubre/uretimTarihi=2016/01/01-son kullanim Tarihi=2018/01/01-ureticiFirma=MUGURLU/TURKEY";

        QRCode barcode = new QRCode();
        barcode.setBarcodeHeight(300);
        barcode.setBarcodeWidth(300);
        barcode.setLeftMargin(10);
        barcode.setRightMargin(10);
        barcode.setBottomMargin(10);
        barcode.setTopMargin(10);
        barcode.setResizeImage(true);
        barcode.setData(data);
        try {
            barcode.renderBarcode(data + ".gif");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        byte[] barcodeBytes = new byte[0];
        try {
            barcodeBytes = barcode.renderBarcodeToBytes();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        StokQRCode cd = new StokQRCode();
        //        s.setStok(yukle(Stok.class, 378));
        cd.setDosyaAdi("TTK_" + data);
        cd.setData(barcodeBytes);
        kaydet(cd);
        s.setStokQRCode(cd);
        kaydet(s);


//        // Create Java QRCode object
//        QRCode barcode = new QRCode();
//
//        // Set QRCode data text to encode
//        barcode.setData("Create-QR-Code-in-Java");
//
//        // Generate QRCode barcode & print into Graphics2D object
//        barcode.drawBarcode("Java Graphics2D object");
//
//        // Generate QRCode barcode & encode into GIF format
//        barcode.drawBarcode("C://barcode-qrcode.gif");
//
//        // Generate QRCode barcode & encode into JPEG format
//        barcode.drawBarcode("C://barcode-qrcode.jpg");
//
//        // Generate QRCode barcode & encode into EPS
//        barcode.drawBarcode2EPS("C://barcode-qrcode.eps");


//        List<FirmaMusteri> firmalar = dao.islemGormeyanFirmalar();
//        for (FirmaMusteri firmaMusteri : firmalar) {
//            System.out.println(firmaMusteri.getIsim() + "********* basladim");
//            flush();
//            dao.temizleHibernate();
//            if (!stokKartlariIslemGormusmu(firmaMusteri)) {
//                TempVeri tempVeri = new TempVeri();
//                tempVeri.setDeger1(firmaMusteri.getId() + "");
//                tempVeri.setDeger2(firmaMusteri.getIsim());
//                kaydet(tempVeri);
//                System.out.println(firmaMusteri.getIsim() + "********* OKEY");
//
//            }
//            System.out.println(firmaMusteri.getIsim() + "********* bitirdim");
//        }


    }


    private boolean stokKartlariIslemGormusmu(FirmaMusteri firmaMusteri) {
        Sorgu sorgu = new Sorgu(Stok.class);
        sorgu.kriterEkle(KriterFactory.esit("firma", firmaMusteri));
        sorgu.kriterEkle(KriterFactory.benzer("grup.kod", "050"));
        List<Stok> stoklar = sorgula(sorgu);
        for (Stok stok : stoklar) {

//            flush();
//            dao.temizleHibernate();
//            Sorgu sorgu2 = new Sorgu(KurumStok.class);
//            sorgu.kriterEkle(KriterFactory.esit("stok", stok));
            System.out.println(stok.getStokKodu() + "********* basladim");
            if (dao.kurumstokDaTanimlimi(stok) == true) {
                return true;
            }
            System.out.println(stok.getStokKodu() + "********* bitirdim");
        }
        return false;
    }


    public String kayitBilgiAl(Long kayitId, String entitiyName) {
        return dao.kayitBilgiAl(kayitId, entitiyName);
    }


    public void temlikOnayla(Temlik temlik, Kullanici kullanici) throws BusinessRuleException {
//        String temlikYeriHesapMuavin = "126000001";
//        String temlikFirmaHesapMuavin = "340000060";
        String firmaHesabininAltNosu = temlik.getMuhatap().getMuhasebe5HesapNo();
        MahsupFis mahsupFis = new MahsupFis(getSimdikiTarih(), kullanici.getAktifKurum(), FisKaynak.STOK, temlik.getMuhatap().getIsim() + " TEMLIK MUHASEBESI");
        muhasebeModulServis.geciciMahsupFisiAc(mahsupFis, kullanici);
        Hesap temlikYeriHesap = muhasebeModulServis.altHesapBulYoksaAc(temlik.TEMLIK_YERI_MUAVIN_HESAP + firmaHesabininAltNosu, "TEMLIK HESABI", kullanici.getAktifKurum());
        Hesap firmaHesap = muhasebeModulServis.altHesapBulYoksaAc(temlik.TEMLIK_FIRMA_MUAVIN_HESAP + firmaHesabininAltNosu, "TEMLIK HESABI", kullanici.getAktifKurum());
        mahsupFis.borcEkle(temlikYeriHesap, temlik.getTutar(), "TEMLIK TUTARI", temlik.getOdemeTarihi());
        mahsupFis.alacakEkle(firmaHesap, temlik.getTutar(), "TEMLIK TUTARI", temlik.getOdemeTarihi());
        muhasebeModulServis.kaliciMahsupFisiAc(mahsupFis);
        temlik.setOnayMuhFisi(mahsupFis);
        temlik.setOnayTarihi(getSimdikiTarih());
        temlik.setAktif(true);
        kaydet(temlik);
    }


    public void temlikIptal(Temlik temlik, Kullanici kullanici) throws BusinessRuleException {
        validateFisVarMi(temlik);
        MahsupFis mahsupFis = new MahsupFis(getSimdikiTarih(), kullanici.getAktifKurum(), FisKaynak.STOK, temlik.getMuhatap().getIsim() + " TEMLIK İPTAL MUHASEBESI");
        muhasebeModulServis.geciciMahsupFisiAc(mahsupFis, kullanici);
        List<FisHareket> fisHareketler = temlik.getOnayMuhFisi().getFisHareketleri();
        for (FisHareket fisHareket : fisHareketler) {
            mahsupFis.hareketEkle(fisHareket.getHesap(), fisHareket.getAlacak(), fisHareket.getBorc(), temlik.getOnayMuhFisi().getFisNo() + " NOLU FİŞİN İPTALİ");
        }
        muhasebeModulServis.kaliciMahsupFisiAc(mahsupFis);
        temlik.setIptalMuhFisi(mahsupFis);
        temlik.setIptalTarihi(getSimdikiTarih());
        temlik.setAktif(false);
        kaydet(temlik);

    }

    private void validateFisVarMi(Temlik temlik) throws BusinessRuleException {
        Sorgu sorgu = new Sorgu(SatisFis.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", temlik.getMuhatap().getKurum()));
        sorgu.kriterEkle(KriterFactory.esit("temlik", temlik));
        sorgu.kriterEkle(KriterFactory.esit("yil", temlik.getTemlikTarihi().getYil()));
        sorgu.kriterEkle(KriterFactory.esit("kapali", true));
        sorgu.kriterEkle(KriterFactory.bos("iptalEdenFis"));
        if (sorgula(sorgu).size() > 0) {
            throw new BusinessRuleException(StokHataKodu.SATIS_FISLERI_OLAN_TEMLIK_IPTAL_EDEMEZSINIZ);
        }
    }


    public void stokKartlariKontrol(Kurum kurum, Tarih tarih) throws BusinessRuleException {
        List<StokBilgi> stokBilgiler = stokYilbasiKontrol(kurum, true);
        BigDecimal sonuc = BigDecimal.ZERO;
        BigDecimal muhToplam = BigDecimal.ZERO;
        for (StokBilgi stokBilgi : stokBilgiler) {
            flush();
            if (EkoopUtils.isBuyuk(stokBilgi.getTutar(), BigDecimal.ZERO)) {
                sonuc = sonuc.add(stokBilgi.getTutar());
            }
            if (stokBilgi.getHatali().equals("HATALI")) {
                throw new BusinessRuleException(StokHataKodu.STOKLARI_KONTROL_ETMELISINIZ);
            }
        }

        muhToplam = getStokKebir150Ve153MuhasebeHesapBakiye(kurum, tarih);
        BigDecimal farkTutar = EkoopUtils.cikar(sonuc, muhToplam);
        if (!EkoopUtils.isEsit(farkTutar, BigDecimal.ZERO)) {
            throw new BusinessRuleException(StokHataKodu.STOKLARI_KONTROL_ETMELISINIZ);
        }

    }

    public EfaturaSayac eFaturaSayacAl(Kurum kurum) throws SQLException, BusinessRuleException {
        return commonServis.eFaturaSayacAl(kurum);
    }

    public void EFaturaGIBeGonder(Kurum kurum, Kullanici kullanici, Long stokFisId) throws BusinessRuleException, SQLException {
        StokFis stokFis = yukle(StokFis.class, stokFisId);

        if (!canEnterStokCriticalSection(kurum, "StokEfaturaGonder", stokFisId.toString(), true)) {
            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        }

        if (stokFis instanceof SatisFis) {
            SatisFis satisFis = yukle(SatisFis.class, stokFisId);
            if (satisFis.getOzetFatura() == null) {
                stokSatisFisEfaturaGonder(kurum, kullanici, satisFis);
            }

        }


        if (stokFis instanceof AlistanIadeCikisFis) {

            AlistanIadeCikisFis alistanIadeCikisFis = yukle(AlistanIadeCikisFis.class, stokFisId);
            if (alistanIadeCikisFis.getOzetFatura() == null) {
                stokAlistanIadeFisEfaturaGonder(kurum, kullanici, alistanIadeCikisFis);
            }
        }

        if (stokFis instanceof AlisFiyatIndirimFis) {

            AlisFiyatIndirimFis alisFiyatIndirimFis = yukle(AlisFiyatIndirimFis.class, stokFisId);
            if (alisFiyatIndirimFis.getOzetFatura() == null) {
                stokAlisFiyatIndirimFisEfaturaGonder(kurum, kullanici, alisFiyatIndirimFis);
            }
        }

        if (stokFis instanceof SatisFiyatArttirimFis) {

            SatisFiyatArttirimFis satisFiyatArttirimFis = yukle(SatisFiyatArttirimFis.class, stokFisId);
            if (satisFiyatArttirimFis.getOzetFatura() == null) {
                stokSatisFiyatArtirimFisEfaturaGonder(kurum, kullanici, satisFiyatArttirimFis);
            }
        }


//        getSatisFisYonetici().getEFaturaGIBeGonder(kullanici, stokFisId);
    }

    private void stokSatisFiyatArtirimFisEfaturaGonder(Kurum kurum, Kullanici kullanici, SatisFiyatArttirimFis satisFiyatArttirimFis) throws BusinessRuleException {
        List<EfaturaEkoopHareket> faturaHareketler = new ArrayList<EfaturaEkoopHareket>();
        List<SatisFiyatArttirimFisHareket> hareketler = satisFiyatArttirimFis.getHareketler();
        int siraNo = 0;
        for (SatisFiyatArttirimFisHareket hareket : hareketler) {
            EfaturaEkoopHareket faturaHareket = new EfaturaEkoopHareket();
            siraNo += 1;
            faturaHareket.setAciklama(hareket.getAciklama());
            faturaHareket.setAliciMalHizmetTanim(hareket.getStok().getStok().getStokKodu());
            faturaHareket.setBirim(WebOlcuBirimKodu.fromValue(hareket.getStok().getStok().getBirim().getuKod()));
            faturaHareket.setBirimFiyat(hareket.getFarkBirimFiyat());


//emita sınıf bilgisi oladığı için setlenmedi
//            private String emtiaSiniflandirmaBilgisi;

            faturaHareket.setKdvMatrahi(hareket.getTutar());


//tevkifat olmadiği icin setlenmedi olunca setle
//            private BigDecimal tevfikatTutari;
            if (hareket.getKdvTutar() == BigDecimal.ZERO && hareket.getStok().getSatisKdvOrani().getOran() != BigDecimal.ZERO) {
                throw new BusinessRuleException(StokHataKodu.STOK_KARTININ_KDV_ORANI_ILE_STOK_HAREKETININ_KDV_TUTARI_TUTARSIZ);
            }

            if (hareket.getStok().getSatisKdvOrani().getOran().multiply(new BigDecimal(100)) == BigDecimal.ZERO) {
                //burayı boyle yas sebebi ne ise onu setle
                faturaHareket.setKdvMuafiyetSebebi("6663 sayılı Kanun’un 13’üncü maddesiyle tam istisna");
            }

            faturaHareket.setKdvOrani(hareket.getStok().getSatisKdvOrani().getOran().multiply(new BigDecimal(100)));
            faturaHareket.setKdvTutari(hareket.getKdvTutar());


            faturaHareket.setMalHizmetAciklama(hareket.getStokAdi());
            faturaHareket.setMalHizmetAdi(hareket.getStokAdi());
            faturaHareket.setMalHizmetMarka(hareket.getStokAdi());
            faturaHareket.setMalHizmetModel(hareket.getStokAdi());
            faturaHareket.setMalHizmetTutari(hareket.getTutar());
            faturaHareket.setMiktar(hareket.getMiktar());
            faturaHareket.setSaticiMalHizmetTanim("SATIŞ");
            faturaHareket.setSiraNo(siraNo);


            faturaHareket.setToplamTutar(hareket.getTutar());

            faturaHareket.setUreticiMalHizmetTanim(hareket.getStok().getStok().getStokAdi());

            faturaHareketler.add(faturaHareket);

        }
        OzetEfatura ozetEfatura = borcAlacakModulServis.getEfaturaOlusturGIBeGonder(kurum, kullanici, satisFiyatArttirimFis.getMuhatap(), WebFaturaTipi.SATIS, WebSenaryoTipi.TEMEL_FATURA, satisFiyatArttirimFis.getFisTarihi(), ModulAdi.STOK, satisFiyatArttirimFis.getAciklama(), faturaHareketler, satisFiyatArttirimFis.getVadeTarih());
        ozetEfatura.setStokId(satisFiyatArttirimFis.getId());
        satisFiyatArttirimFis.setOzetFatura(ozetEfatura);
        satisFiyatArttirimFis.seteFatKullanici(kullanici);
        satisFiyatArttirimFis.seteFatIslmTarih(getSimdikiTarih());
        Fatura fatura = satisFiyatArttirimFis.getFatura();
        fatura.setFaturaNo(ozetEfatura.getEfaturaNo());
        fatura.setFaturaSeriNo("TTK");
        satisFiyatArttirimFis.setFatura(fatura);
        kaydet(ozetEfatura);
        kaydet(satisFiyatArttirimFis);

    }

    private void stokAlisFiyatIndirimFisEfaturaGonder(Kurum kurum, Kullanici kullanici, AlisFiyatIndirimFis alisFiyatIndirimFis) throws BusinessRuleException {
        List<EfaturaEkoopHareket> faturaHareketler = new ArrayList<EfaturaEkoopHareket>();
        List<AlisFiyatIndirimFisHareket> hareketler = alisFiyatIndirimFis.getHareketler();
        int siraNo = 0;
        for (AlisFiyatIndirimFisHareket hareket : hareketler) {
            EfaturaEkoopHareket faturaHareket = new EfaturaEkoopHareket();
            siraNo += 1;
            faturaHareket.setAciklama(hareket.getAciklama());
            faturaHareket.setAliciMalHizmetTanim(hareket.getStok().getStok().getStokKodu());
            faturaHareket.setBirim(WebOlcuBirimKodu.fromValue(hareket.getStok().getStok().getBirim().getuKod()));
            faturaHareket.setBirimFiyat(hareket.getBirimFiyat());


//emita sınıf bilgisi oladığı için setlenmedi
//            private String emtiaSiniflandirmaBilgisi;


//tevkifat olmadiği icin setlenmedi olunca setle
//            private BigDecimal tevfikatTutari;
            if (hareket.getKdvTutar() == BigDecimal.ZERO && hareket.getStok().getSatisKdvOrani().getOran() != BigDecimal.ZERO) {
                throw new BusinessRuleException(StokHataKodu.STOK_KARTININ_KDV_ORANI_ILE_STOK_HAREKETININ_KDV_TUTARI_TUTARSIZ);
            }

            if (hareket.getStok().getSatisKdvOrani().getOran().multiply(new BigDecimal(100)) == BigDecimal.ZERO) {
                //burayı boyle yas sebebi ne ise onu setle
                faturaHareket.setKdvMuafiyetSebebi("6663 sayılı Kanun’un 13’üncü maddesiyle tam istisna");
            }

            faturaHareket.setKdvOrani(hareket.getStok().getSatisKdvOrani().getOran().multiply(new BigDecimal(100)));
            faturaHareket.setKdvTutari(hareket.getKdvTutar());


            faturaHareket.setMalHizmetAciklama(hareket.getStokAdi());
            faturaHareket.setMalHizmetAdi(hareket.getStokAdi());
            faturaHareket.setMalHizmetMarka(hareket.getStokAdi());
            faturaHareket.setMalHizmetModel(hareket.getStokAdi());

            faturaHareket.setMiktar(hareket.getIptalMiktar());
            faturaHareket.setSaticiMalHizmetTanim("SATIŞ");
            faturaHareket.setSiraNo(siraNo);
            BigDecimal kdvliTutar = BigDecimal.ZERO;
            List<FisHareket> muhFisHareketler = alisFiyatIndirimFis.getMuhasebeFisi().getFisHareketleri();
            for (FisHareket fisHareket : muhFisHareketler) {
                if (fisHareket.getHesap().getHesapNo().startsWith("320")) {
                    kdvliTutar = fisHareket.getBorc();
                }
            }

            faturaHareket.setToplamTutar(kdvliTutar);
            faturaHareket.setKdvMatrahi(kdvliTutar.subtract(hareket.getKdvTutar()));
            faturaHareket.setMalHizmetTutari(kdvliTutar.subtract(hareket.getKdvTutar()));


            faturaHareket.setUreticiMalHizmetTanim(hareket.getStok().getStok().getStokAdi());

            faturaHareketler.add(faturaHareket);

        }
        OzetEfatura ozetEfatura = borcAlacakModulServis.getEfaturaOlusturGIBeGonder(kurum, kullanici, alisFiyatIndirimFis.getMuhatap(), WebFaturaTipi.SATIS, WebSenaryoTipi.TEMEL_FATURA, alisFiyatIndirimFis.getFisTarihi(), ModulAdi.STOK, alisFiyatIndirimFis.getAciklama(), faturaHareketler, alisFiyatIndirimFis.getFatura().getFaturaTarihi());
        ozetEfatura.setStokId(alisFiyatIndirimFis.getId());
        alisFiyatIndirimFis.setOzetFatura(ozetEfatura);
        alisFiyatIndirimFis.seteFatKullanici(kullanici);
        alisFiyatIndirimFis.seteFatIslmTarih(getSimdikiTarih());
        Fatura fatura = alisFiyatIndirimFis.getFatura();
        fatura.setFaturaNo(ozetEfatura.getEfaturaNo());
        fatura.setFaturaSeriNo("TTK");
        alisFiyatIndirimFis.setFatura(fatura);
        kaydet(ozetEfatura);
        kaydet(alisFiyatIndirimFis);

    }

    private void stokAlistanIadeFisEfaturaGonder(Kurum kurum, Kullanici kullanici, AlistanIadeCikisFis alistanIadeCikisFis) throws BusinessRuleException {

        List<EfaturaEkoopHareket> faturaHareketler = new ArrayList<EfaturaEkoopHareket>();
        List<AlistanIadeCikisFisHareket> hareketler = alistanIadeCikisFis.getHareketler();
        int siraNo = 0;
        for (AlistanIadeCikisFisHareket hareket : hareketler) {
            EfaturaEkoopHareket faturaHareket = new EfaturaEkoopHareket();
            siraNo += 1;
            faturaHareket.setAciklama(hareket.getAciklama());
            faturaHareket.setAliciMalHizmetTanim(hareket.getStok().getStok().getStokKodu());
            faturaHareket.setBirim(WebOlcuBirimKodu.fromValue(hareket.getStok().getStok().getBirim().getuKod()));
            faturaHareket.setBirimFiyat(hareket.getBirimFiyat());


//emita sınıf bilgisi oladığı için setlenmedi
//            private String emtiaSiniflandirmaBilgisi;

            faturaHareket.setKdvMatrahi(hareket.getTutar());


//tevkifat olmadiği icin setlenmedi olunca setle
//            private BigDecimal tevfikatTutari;
            if (hareket.getKdvTutar() == BigDecimal.ZERO && hareket.getStok().getSatisKdvOrani().getOran() != BigDecimal.ZERO) {
                throw new BusinessRuleException(StokHataKodu.STOK_KARTININ_KDV_ORANI_ILE_STOK_HAREKETININ_KDV_TUTARI_TUTARSIZ);
            }

            if (hareket.getStok().getSatisKdvOrani().getOran().multiply(new BigDecimal(100)) == BigDecimal.ZERO) {
                //burayı boyle yas sebebi ne ise onu setle
                faturaHareket.setKdvMuafiyetSebebi("6663 sayılı Kanun’un 13’üncü maddesiyle tam istisna");
            }

            faturaHareket.setKdvOrani(hareket.getStok().getSatisKdvOrani().getOran().multiply(new BigDecimal(100)));
            faturaHareket.setKdvTutari(hareket.getKdvTutar());


            faturaHareket.setMalHizmetAciklama(hareket.getStokAdi());
            faturaHareket.setMalHizmetAdi(hareket.getStokAdi());
            faturaHareket.setMalHizmetMarka(hareket.getStokAdi());
            faturaHareket.setMalHizmetModel(hareket.getStokAdi());
            faturaHareket.setMalHizmetTutari(hareket.getTutar());
            faturaHareket.setMiktar(hareket.getMiktar());
            faturaHareket.setSaticiMalHizmetTanim("IADE");
            faturaHareket.setSiraNo(siraNo);


            faturaHareket.setToplamTutar(hareket.getKdvliTutar());

            faturaHareket.setUreticiMalHizmetTanim(hareket.getStok().getStok().getStokAdi());

            faturaHareketler.add(faturaHareket);

        }
        OzetEfatura ozetEfatura = borcAlacakModulServis.getEfaturaOlusturGIBeGonder(kurum, kullanici, alistanIadeCikisFis.getMuhatap(), WebFaturaTipi.IADE, WebSenaryoTipi.TEMEL_FATURA, alistanIadeCikisFis.getFisTarihi(), ModulAdi.STOK, alistanIadeCikisFis.getAciklama(), faturaHareketler, alistanIadeCikisFis.getFatura().getFaturaTarihi());
        ozetEfatura.setStokId(alistanIadeCikisFis.getId());
        alistanIadeCikisFis.setOzetFatura(ozetEfatura);
        alistanIadeCikisFis.seteFatKullanici(kullanici);
        alistanIadeCikisFis.seteFatIslmTarih(getSimdikiTarih());
        Fatura fatura = alistanIadeCikisFis.getFatura();
        fatura.setFaturaNo(ozetEfatura.getEfaturaNo());
        fatura.setFaturaSeriNo("TTK");
        alistanIadeCikisFis.setFatura(fatura);
        kaydet(ozetEfatura);
        kaydet(alistanIadeCikisFis);


    }

    private void stokSatisFisEfaturaGonder(Kurum kurum, Kullanici kullanici, SatisFis satisFis) throws BusinessRuleException {

        List<EfaturaEkoopHareket> faturaHareketler = new ArrayList<EfaturaEkoopHareket>();
        List<SatisFisHareket> hareketler = satisFis.getHareketler();
        int siraNo = 0;
        for (SatisFisHareket hareket : hareketler) {
            EfaturaEkoopHareket faturaHareket = new EfaturaEkoopHareket();
            siraNo += 1;
            faturaHareket.setAciklama(hareket.getAciklama());
            faturaHareket.setAliciMalHizmetTanim(hareket.getStok().getStok().getStokKodu());
            faturaHareket.setBirim(WebOlcuBirimKodu.fromValue(hareket.getStok().getStok().getBirim().getuKod()));


            if (hareket.getStok().isAkaryakit() && satisFis.getKurum().isBolge()) {

                AkaryakitIslem akaryakitIslem = UnProxyEntity.unproxy(AkaryakitIslem.class, ((BolgeSatisFis) satisFis).getAkaryakitIslem().getId());
                faturaHareket.setBirimFiyat(akaryakitIslem.getGirisBirimFiyat());
            } else {
                BigDecimal satisFiyat = hareket.getSatisFiyat().getFiyat();
                if (hareket instanceof KoopSatisFisHareket) {
                    KoopSatisFisHareket koopSatisFisHareket = (KoopSatisFisHareket) hareket;
//                    satisFiyat = EkoopUtils.isBuyuk(koopSatisFisHareket.getYeni SatisFiyat(), BigDecimal.ZERO) ? ((KoopSatisFisHareket) hareket).getYeniSatisFiyat() : hareket.getSatisFiyat().getFiyat();
                }
                faturaHareket.setBirimFiyat(hareket.getTutar().divide(hareket.getMiktar(), 4, RoundingMode.HALF_UP));
            }

//emita sınıf bilgisi oladığı için setlenmedi
//            private String emtiaSiniflandirmaBilgisi;

            faturaHareket.setKdvMatrahi(hareket.getTutar());


//tevkifat olmadiği icin setlenmedi olunca setle
//            private BigDecimal tevfikatTutari;

            if (hareket.getKdvTutar() == BigDecimal.ZERO && hareket.getStok().getSatisKdvOrani().getOran() != BigDecimal.ZERO) {
                throw new BusinessRuleException(StokHataKodu.STOK_KARTININ_KDV_ORANI_ILE_STOK_HAREKETININ_KDV_TUTARI_TUTARSIZ);
            }
            if (hareket.getStok().getSatisKdvOrani().getOran().multiply(new BigDecimal(100)) == BigDecimal.ZERO) {
                //burayı boyle yas sebebi ne ise onu setle
                faturaHareket.setKdvMuafiyetSebebi("6663 sayılı Kanun’un 13’üncü maddesiyle tam istisna");
            }


            faturaHareket.setMalHizmetAciklama(hareket.getStokAdi());
            faturaHareket.setMalHizmetAdi(hareket.getStokAdi());
            faturaHareket.setMalHizmetMarka(hareket.getStokAdi());
            faturaHareket.setMalHizmetModel(hareket.getStokAdi());
            faturaHareket.setMalHizmetTutari(hareket.getTutar());
            faturaHareket.setMiktar(hareket.getMiktar());
            faturaHareket.setSaticiMalHizmetTanim("SATIŞ");
            faturaHareket.setSiraNo(siraNo);
            if (hareket.getToplamIndirimTutar() != null && EkoopUtils.isBuyuk(hareket.getToplamIndirimTutar(), BigDecimal.ZERO)) {
                faturaHareket.setSkontoOrani(hareket.getStok().getSatisIskontoOran());
                faturaHareket.setSkontoTutari(hareket.getToplamIndirimTutar());
                faturaHareket.setMalHizmetTutari(hareket.getTutar().add(hareket.getToplamIndirimTutar()));

            }

            faturaHareket.setToplamTutar(hareket.getMusterininOdeyecegiTutar());

            faturaHareket.setUreticiMalHizmetTanim(hareket.getStok().getStok().getStokAdi());


            if (satisFis.getKurum().isKooperatif() && hareket.getStok().isTohumlukStokMu() && satisFis.getSonTuketiciSatisMi()) {
                faturaHareket.setKdvOrani(BigDecimal.ZERO);
                faturaHareket.setKdvTutari(BigDecimal.ZERO);
                faturaHareket.setMalHizmetTutari(hareket.getMusterininOdeyecegiTutar());
                faturaHareket.setKdvMatrahi(hareket.getMusterininOdeyecegiTutar());
                faturaHareket.setBirimFiyat(hareket.getMusterininOdeyecegiTutar().divide(hareket.getMiktar(), 4, RoundingMode.HALF_UP));
                faturaHareket.setKdvMuafiyetSebebi("Madde 17 – 1. Kültür ve Eğitim Amacı Taşıyan İstisnalar: ");
            } else {
                faturaHareket.setKdvOrani(hareket.getStok().getSatisKdvOrani().getOran().multiply(new BigDecimal(100)));
                faturaHareket.setKdvTutari(hareket.getKdvTutar());
            }


            faturaHareketler.add(faturaHareket);

        }
//        WebSenaryoTipi senaryoTipi = WebSenaryoTipi.TICARI_FATURA;
//        if (satisFis.getKurum().isMerkez()) {
//            senaryoTipi = WebSenaryoTipi.TEMEL_FATURA;
//
//        }
        OzetEfatura ozetEfatura = borcAlacakModulServis.getEfaturaOlusturGIBeGonder(kurum, kullanici, satisFis.getMuhatap(), WebFaturaTipi.SATIS, WebSenaryoTipi.TEMEL_FATURA, satisFis.getFisTarihi(), ModulAdi.STOK, satisFis.getAciklama(), faturaHareketler, satisFis.getSonOdemeTarih());
        ozetEfatura.setStokId(satisFis.getId());
        satisFis.setOzetFatura(ozetEfatura);
        satisFis.seteFatKullanici(kullanici);
        satisFis.seteFatIslmTarih(getSimdikiTarih());
        Fatura fatura = satisFis.getFatura();
        fatura.setFaturaNo(ozetEfatura.getEfaturaNo());
        fatura.setFaturaSeriNo("TTK");
        satisFis.setFatura(fatura);
        if (satisFis.getKaynak().equals(StokFisKaynak.SEVK) && satisFis.getKurum().isMerkez()) {
            sevkFisininFaturasiniGuncelle(satisFis, fatura);
        }
        kaydet(satisFis);
        kaydet(ozetEfatura);
    }

    private void sevkFisininFaturasiniGuncelle(SatisFis fis, Fatura fatura) throws BusinessRuleException {
        Sorgu sorgu = new Sorgu(SevkStokCikisFis.class);
        sorgu.kriterEkle(KriterFactory.esit("stokSatisFis", fis));
        SevkStokCikisFis sevkStokCikisFis = (SevkStokCikisFis) sorgula(sorgu).get(0);
        sevkStokCikisFis.setFatura(fatura);
        kaydet(sevkStokCikisFis);
    }


    public void validateMiktarTutarGiderKontrol(KurumStok kurumStok) throws BusinessRuleException {
        BigDecimal mevcutMiktar = getMevcutStokMiktari(kurumStok);
        BigDecimal mevcutTutar = getMevcutStokTutari(kurumStok);
        if (EkoopUtils.isKucukEsit(mevcutMiktar, BigDecimal.ZERO) || EkoopUtils.isKucukEsit(mevcutTutar, BigDecimal.ZERO)) {
            throw new BusinessRuleException(StokHataKodu.STOKLARI_KONTROL_ETMELISINIZ);
        }

    }


    public KurumBirim getKullanicininAktifKurumBirimi(Kullanici kullanici) {
        return dao.getKullanicininAktifKurumBirimi(kullanici);
    }


    public BigDecimal getKomisyonTutari(Kooperatif kurum, String faturaNo) throws BusinessRuleException {
        Sorgu sorgu = new Sorgu(KoopSatisFis.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        sorgu.kriterEkle(KriterFactory.esit("tip", StokFisTip.KOOP_SATIS));
        sorgu.kriterEkle(KriterFactory.esit("fatura.faturaNo", faturaNo));
        if (!(sorgula(sorgu).size() > 0)) {
            throw new BusinessRuleException(StokHataKodu.FATURA_KURUMU_BOS_OLAMAZ);
        } else {
            KoopSatisFis koopSatisFis = (KoopSatisFis) sorgula(sorgu).get(0);
            return satisMahsupFisindenKarBulma(koopSatisFis.getMuhasebeFisi()).multiply(new BigDecimal(0.8));
        }
    }

    private BigDecimal satisMahsupFisindenKarBulma(Fis muhasebeFisi) {
        BigDecimal rakam600 = BigDecimal.ZERO;
        BigDecimal rakam621 = BigDecimal.ZERO;
        List<FisHareket> fisHareketleri = muhasebeFisi.getFisHareketleri();
        for (FisHareket fisHareket : fisHareketleri) {
            if (fisHareket.getHesap().getHesapNo().startsWith("600")) {
                rakam600 = rakam600.add(fisHareket.getAlacak());
            }

            if (fisHareket.getHesap().getHesapNo().startsWith("621")) {
                rakam621 = rakam621.add(fisHareket.getBorc());
            }
        }

        return rakam600.subtract(rakam621);
    }


    public void extAygitFisiKes(ExtAygitFisCikis extAygitFisCikis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().stokFisKes(extAygitFisCikis, kullanici);
    }


    public void extAygitFisHareketEkle(ExtAygitFisCikis extAygitFisCikis, ExtAygitFisCikisHareket extAygitFisCikisHareket, Kurum kurum) throws BusinessRuleException {
        getSatisFisYonetici().stokCikisHareketEkle(extAygitFisCikis, extAygitFisCikisHareket);
    }


    public void extAygitFisOnayla(ExtAygitFisCikis extAygitFisCikis, Kullanici kullanici) throws BusinessRuleException {
        getSatisFisYonetici().extAygitFisOnayla(extAygitFisCikis, kullanici);
    }

    public boolean eFaturaStokaAktar(List<WebFaturaTtk> efaturalar, Kullanici kullanici) throws BusinessRuleException {
        for (WebFaturaTtk webFaturaTtk : efaturalar) {
            flush();
            if (!canEnterStokCriticalSection(kullanici.getAktifKurum(), "EFaturaAKTARMA", webFaturaTtk.getFaturaNo(), true)) {
                throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
            }
            if (webFaturaTtk.getFaturaNo().startsWith("GFB")) {
                eFaturaStokaAl(webFaturaTtk, kullanici);
            }

            if (webFaturaTtk.getFaturaNo().startsWith("BCS")) {
                efaturaBayerStokIslem(webFaturaTtk, kullanici);
            }

            WebFaturaTtk webFatura2 = yukle(WebFaturaTtk.class, webFaturaTtk.getId());
//            webFatura2.setFaturaKabulEdildi(Boolean.TRUE);
//            webFatura2.setKontrolEdenKullanici(kullanici);
            webFatura2.setAktarimDurum(AktarimDurum.STOK);
            kaydet(webFatura2);
        }

        return true;
    }

    private void eFaturaStokaAl(WebFaturaTtk webFaturaTtk, Kullanici kullanici) throws BusinessRuleException {


        // Gubretas firmasini
//        FirmaMusteri firmaMusteri = yukle(FirmaMusteri.class, 53);
        FirmaMusteri firmaMusteri = vergiNodanFirmaMusteriBul(webFaturaTtk.getAlanKurum(), webFaturaTtk.getVergiTcKimlikNo());

//

        FaturaFis faturaFis = new FaturaFis();
        faturaFis.setFatura(new Fatura("GFB", webFaturaTtk.getFaturaNo(), webFaturaTtk.getFaturaTarihi(), webFaturaTtk.getOdenecekTutar()));
        faturaFis.setFisTarihi(getSimdikiTarih());

        //termin ay ve yil larak siparis tarihinden aliyoruz burda nul hatası alıyoruz..

        String ay = webFaturaTtk.getSiparisNumarasi().substring(webFaturaTtk.getSiparisNumarasi().length() - 2, webFaturaTtk.getSiparisNumarasi().length());
        String yil = webFaturaTtk.getSiparisNumarasi().substring(webFaturaTtk.getSiparisNumarasi().length() - 7, webFaturaTtk.getSiparisNumarasi().length() - 3);
        faturaFis.setTerminAyi(Ay.ayNodanBul(Integer.valueOf(ay)));
        faturaFis.setTerminYili(Integer.valueOf(yil));

        faturaFis.setMuhatap(firmaMusteri);
        faturaFis.setValorTarihi(webFaturaTtk.getSonOdemeTarihi());

        if (webFaturaTtk.getIrsaliyeler().size() > 0) {
            faturaFis.setAciklama("EFATURA AKTARIM " + webFaturaTtk.getIrsaliyelerAciklama());
        } else {
            faturaFis.setAciklama("EFATURA AKTARIM ");
        }

        faturaFis.setVeridenMi(false);
        faturaFis.setTeslimTarihi(webFaturaTtk.getFaturaTarihi());
        faturaFis.setFisKonuAyrac(FisKonuAyrac.GUBRE);
        faturaFisKes(faturaFis, kullanici);
        String stokKodu = webFaturaTtk.getFaturaHareketleri().get(0).getAliciMalHizmetTanim();
        Sorgu sorgu = new Sorgu(KurumStok.class, null, KriterFactory.esit("stok.stokKodu", stokKodu));
        Kurum mB = yukle(Kurum.class, 195);
        sorgu.kriterEkle(KriterFactory.esit("kurum", mB));
        KurumStok kurumStok = (KurumStok) sorgula(sorgu).get(0);


        BigDecimal tutar = webFaturaTtk.getFaturaHareketleri().get(0).getKdvMatrahi();
        BigDecimal miktar = webFaturaTtk.getFaturaHareketleri().get(0).getMiktar();
        BigDecimal kdvTutar = webFaturaTtk.getFaturaHareketleri().get(0).getKdvTutari();
        BigDecimal birimFiyat = webFaturaTtk.getFaturaHareketleri().get(0).getBirimFiyat();

        FaturaFisHareket faturaFisHareket = new FaturaFisHareket(kurumStok, null,
                null, null, miktar, tutar,
                miktar, "Giriş efatura hareket");
        faturaFisHareket.setFaturaBirimFiyat(birimFiyat);
        FaturaKdvBilgi faturaKdvBilgi = new FaturaKdvBilgi();
        faturaKdvBilgi.setOran(kurumStok.getAlisKdvOrani());
        faturaKdvBilgi.setTutar(kdvTutar);
        faturaFis.getKdvBilgileri().add(faturaKdvBilgi);
        faturaFisHareketEkle(faturaFis, faturaFisHareket);

        getStokFisYonetici().hareketlerideKapaliVeAyYilDuzenle(faturaFis);
        fisOnayla(faturaFis);
    }


    public void efaturaBayerStokIslem(WebFaturaTtk webFaturaTtk, Kullanici kullanici) throws BusinessRuleException {
        FirmaMusteri firmaMusteri = vergiNodanFirmaMusteriBul(webFaturaTtk.getAlanKurum(), webFaturaTtk.getVergiTcKimlikNo());

//

        FaturaFis faturaFis = new FaturaFis();
        faturaFis.setFatura(new Fatura(webFaturaTtk.getFaturaNo().substring(0, 3), webFaturaTtk.getFaturaNo(), webFaturaTtk.getFaturaTarihi(), webFaturaTtk.getOdenecekTutar()));
        faturaFis.setFisTarihi(getSimdikiTarih());

        //termin ay ve yil larak siparis tarihinden aliyoruz burda nul hatası alıyoruz..

        String ay = webFaturaTtk.getSiparisNumarasi().substring(webFaturaTtk.getSiparisNumarasi().length() - 2, webFaturaTtk.getSiparisNumarasi().length());
        String yil = webFaturaTtk.getSiparisNumarasi().substring(webFaturaTtk.getSiparisNumarasi().length() - 7, webFaturaTtk.getSiparisNumarasi().length() - 3);
        faturaFis.setTerminAyi(Ay.ayNodanBul(Integer.valueOf(ay)));
        faturaFis.setTerminYili(Integer.valueOf(yil));

        faturaFis.setMuhatap(firmaMusteri);
        faturaFis.setValorTarihi(webFaturaTtk.getSonOdemeTarihi());

        if (webFaturaTtk.getIrsaliyeler().size() > 0) {
            faturaFis.setAciklama("EFATURA AKTARIM " + webFaturaTtk.getIrsaliyelerAciklama());
        } else {
            faturaFis.setAciklama("EFATURA AKTARIM ");
        }

        faturaFis.setVeridenMi(false);
        faturaFis.setTeslimTarihi(webFaturaTtk.getFaturaTarihi());
        faturaFis.setFisKonuAyrac(FisKonuAyrac.DIGER);
        faturaFisKes(faturaFis, kullanici);


        String satıciMamulKodu = webFaturaTtk.getFaturaHareketleri().get(0).getSaticiMalHizmetTanim();
        Sorgu sorgu = new Sorgu(KurumStok.class, null, KriterFactory.esit("stok.saticiMamulKodu", satıciMamulKodu));
        Kurum mB = yukle(Kurum.class, 195);
        sorgu.kriterEkle(KriterFactory.esit("kurum", mB));
        KurumStok kurumStok = (KurumStok) sorgula(sorgu).get(0);


        BigDecimal tutar = webFaturaTtk.getFaturaHareketleri().get(0).getKdvMatrahi();
        BigDecimal miktar = webFaturaTtk.getFaturaHareketleri().get(0).getMiktar();
        BigDecimal kdvTutar = webFaturaTtk.getFaturaHareketleri().get(0).getKdvTutari();
        BigDecimal birimFiyat = webFaturaTtk.getFaturaHareketleri().get(0).getBirimFiyat();

        FaturaFisHareket faturaFisHareket = new FaturaFisHareket(kurumStok, null,
                null, null, miktar, tutar,
                miktar, "Giriş efatura hareket");
        faturaFisHareket.setFaturaBirimFiyat(birimFiyat);
        FaturaKdvBilgi faturaKdvBilgi = new FaturaKdvBilgi();
        faturaKdvBilgi.setOran(kurumStok.getAlisKdvOrani());
        faturaKdvBilgi.setTutar(kdvTutar);
        faturaFis.getKdvBilgileri().add(faturaKdvBilgi);
        faturaFisHareketEkle(faturaFis, faturaFisHareket);

        getStokFisYonetici().hareketlerideKapaliVeAyYilDuzenle(faturaFis);
        fisOnayla(faturaFis);

        //çıkış işlemini koy


    }


    private FirmaMusteri vergiNodanFirmaMusteriBul(Kurum kurum, String vergiTcKimlikNo) {
        Sorgu sorgu = new Sorgu(TuzelFirmaMusteri.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        sorgu.kriterEkle(KriterFactory.esit("vergiNo", vergiTcKimlikNo));
        return (FirmaMusteri) sorgula(sorgu).get(0);
    }


    public void muhasebeKarsilastir(Kurum kurum) throws BusinessRuleException {
        List<Hesap> hesaplar = dao.getKurumStokKartlariMuhasebeHesaplari(kurum);
        for (Hesap hesap : hesaplar) {
            flush();
            BigDecimal hesapBakiye = muhasebeModulServis.getAltHesapBakiyesi(kurum, hesap, 2015).getAbsoluteBakiye();
            System.out.println(hesap.getHesapNo() + "*********hno geldi---" + hesapBakiye);
            BigDecimal stoklarinBakiyesi = hesabaBagliStokKarlarinBakiyesi(kurum, hesap);
            TempVeri tVrei = new TempVeri();
            tVrei.setDeger1(hesap.getHesapNo());
            tVrei.setDecDeger1(hesapBakiye);
            tVrei.setDecDeger2(stoklarinBakiyesi);
            System.out.println(hesap.getHesapNo() + "*********STK BAKİYE---" + stoklarinBakiyesi);
            kaydet(tVrei);
        }
    }


    public void stokFisMahsupsuzOnayla(StokFis stokFis, Kullanici kullanici) throws BusinessRuleException {
        getStokFisYonetici().kaliciFisNoVer(stokFis);
    }

    private BigDecimal hesabaBagliStokKarlarinBakiyesi(Kurum kurum, Hesap hesap) {
        BigDecimal sonuc = BigDecimal.ZERO;
        Sorgu sorgu = new Sorgu(KurumStok.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        sorgu.kriterEkle(KriterFactory.esit("stok.stokMuhasebeHesabi", hesap));
        List<KurumStok> kurumStoklar = sorgula(sorgu);

        for (KurumStok kurumStok : kurumStoklar) {
            flush();
            BigDecimal mevTutar = getMevcutStokTutari(kurumStok);
            sonuc = sonuc.add(mevTutar);

        }

        return sonuc;
    }


    public List<GubretasVeri> gubretasVerilerListesi(String kurumNo, String stokKodu) {
        return dao.gubretasVerilerListesi(kurumNo, stokKodu);
    }


    public KartWSSonuc posGetProducts(String koopNo) {
        KartWSSonuc sonuc = new KartWSSonuc();
        Sorgu sorgu = new Sorgu(KurumStok.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum.kurumNo", koopNo));
//        sorgu.kriterEkle(KriterFactory.benzer("stok.grup.kod","040"));
        List<KurumStok> stoklar = sorgula(sorgu);
        if (stoklar.size() > 0) {
            sonuc.setSonucKodu("02");
            sonuc.setSonucAciklama("Basarili işlem");
            Product[] urunler = new Product[stoklar.size()];
            int ind = 0;
            for (KurumStok stok : stoklar) {
                String stokKodu = stok.getStok().getStokAdi().length() > 8 ? stok.getStok().getStokAdi().substring(0, 8) : stok.getStok().getStokAdi();
                String takipOzelik = "0";
                if (stok.getTakipOzellikTip().equals(TakipOzellikTip.SARJNO_SARJTARIHI_SONKULLANMATARIHI) ||
                        stok.getTakipOzellikTip().equals(TakipOzellikTip.SERTIFIKANO_SERTIFIKATARIHI) ||
                        stok.getTakipOzellikTip().equals(TakipOzellikTip.PARTINO_PARTITARIHI_SONKULLANMATARIHI)) {
                    takipOzelik = "1";
                }

                Product urun = new Product(stok.getStok().getStokKodu(), stokKodu, takipOzelik);
                urunler[ind] = urun;
                ind++;
            }
            sonuc.setProducts(urunler);
        }
        return sonuc;
    }


    public KartWSSonuc posNakitHesapla(String koopNo, String ortakNo, PosKart[] posKartlar) {
        KartWSSonuc sonuc = new KartWSSonuc();
        if (!kartVerilmeKontrol(koopNo, ortakNo)) {
            sonuc.setSonucKodu("03");
            sonuc.setSonucAciklama("Basarisiz.OrtakKartIslemiYapilmamis");
            return sonuc;
        }

        for (PosKart posKart : posKartlar) {
            BigDecimal satisFiyati = BigDecimal.ZERO;
            try {
                KurumStok kurumStok = getKurumStokKarti(getStokKarti(posKart.getStokKodu()), commonServis.getKurumByNo(koopNo));
                satisFiyati = getSatisFiyatKoop(kurumStok, getSimdikiTarih(), MuhatapTip.ORTAK_ICI).getFiyat();
                sonuc.setSonucKodu("02");
                sonuc.setSonucAciklama("Basarili");
            } catch (Exception e) {
                sonuc.setSonucKodu("03");
                sonuc.setSonucAciklama("Basarisiz.FiyatAlinamdi");
                e.printStackTrace();
            }
            double hareketMotTutar = EkoopUtils.tutarCarp(satisFiyati, new BigDecimal(posKart.getMiktar())).doubleValue();
            posKart.setToplamTutar(hareketMotTutar);

        }
        sonuc.setPosKartlar(posKartlar);
        return sonuc;


    }

    public boolean kartVerilmeKontrol(String koopNo, String ortakNo) {
        Sorgu sorgu = new Sorgu(Ortak.class);
        sorgu.kriterEkle(KriterFactory.esit("no", ortakNo));
        sorgu.kriterEkle(KriterFactory.esit("kurum.kurumNo", koopNo));
        List<Ortak> ortaklar = sorgula(sorgu);
        if (ortaklar.size() > 0) {
            if (ortaklar.get(0).isOrtakKartVerildimi()) {
                return true;
            } else {
                return false;
            }

        }

        return false;
    }


    public EfaturaSayac efaturaSayacArttir(Kurum aktifKurum, Integer yil, Tarih simdikiTarih) {
        return dao.efaturaSayacArttir(aktifKurum, yil, simdikiTarih);
    }


    public KartWSSonuc poslaStokCikisiYap(String koopNo, String ortakNo, PosKart[] posKartlar, Kullanici posKullanici) {
        return getSatisFisYonetici().poslaStokCikisiYap(koopNo, ortakNo, posKartlar, posKullanici);
    }

    public KartWSSonuc poslaNakitSatisIptal(String koopNo, String ortakNo, String stokFisNo) {
        return getSatisFisYonetici().poslaNakitSatisIptal(koopNo, ortakNo, stokFisNo);
    }

    public String getEFaturaKullaniciAdi(AbstractKurum kurum) throws BusinessRuleException {
        return commonServis.getEFaturaKullaniciAdi(kurum);
    }

    public String getEFaturaKullaniciSifre(AbstractKurum kurum) throws BusinessRuleException {
        return commonServis.getEFaturaKullaniciSifre(kurum);
    }


    public String mobilStokSorgulama(String kartNo, String stokKodu, String stokAdi) {

//        return "Mobil Stok Sorgulama Cagirildi";

        if (kartNo.length() != 16) {
            return "Hatali Kart Numarasi";
        }


        String koopNo = "K" + kartNo.substring(4, 10);
        int ortakNoInt = Integer.valueOf(kartNo.substring(10, 16));
        Sorgu kurumSorgu = new Sorgu(Kooperatif.class);
        kurumSorgu.kriterEkle(KriterFactory.esit("kurumNo", koopNo));
        Kurum kurum = (Kurum) sorgula(kurumSorgu).get(0);

        Sorgu ortaksorgu = new Sorgu(Ortak.class);
        ortaksorgu.kriterEkle(KriterFactory.esit("no", ortakNoInt + ""));
        ortaksorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        Ortak ortak = (Ortak) sorgula(ortaksorgu).get(0);

        Sorgu kurumStok = new Sorgu(KurumStok.class);
        kurumStok.kriterEkle(KriterFactory.esit("kurum", kurum));
        if (!stokKodu.equals("0")) {
            kurumStok.kriterEkle(KriterFactory.esit("stok.stokKodu", stokKodu));
        }

        if (!stokAdi.equals("0")) {
            kurumStok.kriterEkle(KriterFactory.benzer("stok.stokAdi", stokAdi));
        }

        if (sorgula(kurumStok).size() > 0) {
            return "Sayin " + ortak.getIsim() + " Girmiş olduğunuz stok kooperatifinizde bulunabilir.Kooperatifinizle iletişime geçiniz.. ";

        }


        return "Sayin " + ortak.getIsim() + " Girmiş olduğunuz stok kooperatifinizde bulunmamaktadır.. ";

    }

    public boolean stokPasKontrolVarMi(Kurum kurum) {
        Sorgu sorgu = new Sorgu(StokPasKontrol.class);
        sorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        sorgu.kriterEkle(KriterFactory.buyuk("bitisTarihi", getSimdikiTarih()));
        List<StokPasKontrol> pasKontroller = sorgula(sorgu);
        if (pasKontroller.size() > 0) {
            for (StokPasKontrol stokPasKontrol : pasKontroller) {
                if (getSimdikiTarih().after(stokPasKontrol.getBaslangicTarihi())) {
                    return true;
                }
            }

        }
        return false;
    }


    public void merkeziSatisFiyatDosyaYukle(InputStream inputStream, Kullanici kullanici) throws BusinessRuleException {

        Scanner scanner = new Scanner(inputStream);
        int satirNo = 0;
        while (scanner.hasNextLine()) {
            ++satirNo;
            try {
                merkeziSatisFiyatSatirIsle(scanner.nextLine(), kullanici);
            } catch (Exception e) {
                throw new BusinessRuleException(SevkHataKodu.AKTARIM_BASARISIZ, Integer.toString(satirNo));
            }
        }

        scanner.close();


    }

    //merkezi fiayatta çalisan kod

    private void merkeziSatisFiyatSatirIsle1(String aLine, Kullanici kullanici) throws BusinessRuleException {

        Scanner scanner = new Scanner(aLine);
        scanner.useDelimiter("&");
        if (scanner.hasNext()) {
            String stokNo = scanner.next();
            String gecerliTarih = scanner.next();
            String gecerliIl = scanner.next().trim();
            String fiyat = scanner.next();
//            String tabanFiyat = scanner.next();
            Il il = commonServis.getIlByIlkodu(gecerliIl.trim());
            Sorgu sorgu = new Sorgu(Stok.class, null, KriterFactory.esit("stokKodu", stokNo));
            Stok stok = new Stok();
            if (sorgula(sorgu).size() > 0) {
                stok = (Stok) sorgula(sorgu).get(0);
                if (!stok.getMerkeziFiyatMi()) {
                    throw new BusinessRuleException(StokHataKodu.MERKEZI_FIYAT_BULUNAMADI);
                }
                stokunAktifMerkeziFiyatiniPasifYap(stok, il);

            } else {
                throw new BusinessRuleException(StokHataKodu.STOK_KARTINI_TANIMLAMANIZ_GEREKIR);
            }


            MerkeziFiyat merkeziFiyat = new MerkeziFiyat();
            merkeziFiyat.setStok(stok);
            merkeziFiyat.setFiyat(new BigDecimal(fiyat));
            merkeziFiyat.setGecerliIl(il);
            merkeziFiyat.setBaslamaTarihi(new Tarih(gecerliTarih));
            merkeziFiyat.setTabanFiyat(new BigDecimal("0"));

            kaydet(merkeziFiyat);
        }

        scanner.close();
    }

    //merkezi fiayatta geri alinan kod
    private void merkeziSatisFiyatSatirIsle(String aLine, Kullanici kullanici) throws BusinessRuleException {

        Scanner scanner = new Scanner(aLine);
        scanner.useDelimiter("&");
        if (scanner.hasNext()) {

            String kurumno = scanner.next();
            String stokNo = scanner.next();
            String gecerliTarih = scanner.next();
            String fiyat = scanner.next();
            String fiyatTipi = scanner.next();
            String odemeSekli = scanner.next();
            String gecerliIl = scanner.next().trim();
            Il il = commonServis.getIlByIlkodu(gecerliIl.trim());
            Sorgu sorgu = new Sorgu(Stok.class, null, KriterFactory.esit("stokKodu", stokNo));
            Stok stok = new Stok();
            if (sorgula(sorgu).size() > 0) {
                stok = (Stok) sorgula(sorgu).get(0);
                if (!stok.getMerkeziFiyatMi()) {
                    throw new BusinessRuleException(StokHataKodu.MERKEZI_FIYAT_BULUNAMADI);
                }
                stokunAktifMerkeziFiyatiniPasifYap(stok, il);

            } else {
                throw new BusinessRuleException(StokHataKodu.STOK_KARTINI_TANIMLAMANIZ_GEREKIR);
            }
            BigDecimal tavanFiyat = BigDecimal.ZERO;

            MerkeziFiyat merkeziFiyat = new MerkeziFiyat();
            merkeziFiyat.setStok(stok);
            tavanFiyat = new BigDecimal(fiyat);
            BigDecimal iskontoTutar = BigDecimal.ZERO;


            if (iskontoUygulanacakmi(stok)) {
                //2017 yılı için iskonto orani %3,5 diye bildirildi
                iskontoTutar = EkoopUtils.tutarCarp(new BigDecimal(fiyat), new BigDecimal("0.015"));
            }

            tavanFiyat = tavanFiyat.subtract(iskontoTutar);


            tavanFiyat = tavanFiyat.add(EkoopUtils.tutarCarp(tavanFiyat, getParametre("GUBRE_SATIS_TAVAN_FIYAT_ORANI", true).getBigDecimalDeger()));

            merkeziFiyat.setFiyat(tavanFiyat);

            merkeziFiyat.setGecerliIl(il);
            merkeziFiyat.setBaslamaTarihi(new Tarih(gecerliTarih));


            BigDecimal tabanFiyat = BigDecimal.ZERO;

            tabanFiyat = new BigDecimal(fiyat);
            tabanFiyat = tabanFiyat.subtract(iskontoTutar);
            //398329 nolu talebe istinaden asagidaki kod eklendi
            //496600 nolu talep ile eski haline getirildi
//            if (stokNo.equals("268")) {
//                tabanFiyat = tabanFiyat.add(EkoopUtils.tutarCarp(tabanFiyat, new BigDecimal("0.015")));
//            } else {
//                tabanFiyat = tabanFiyat.add(EkoopUtils.tutarCarp(tabanFiyat, getParametre("GUBRE_SATIS_TABAN_FIYAT_ORANI", true).getBigDecimalDeger()));
//            }
            tabanFiyat = tabanFiyat.add(EkoopUtils.tutarCarp(tabanFiyat, getParametre("GUBRE_SATIS_TABAN_FIYAT_ORANI", true).getBigDecimalDeger()));


            merkeziFiyat.setTabanFiyat(tabanFiyat);

            kaydet(merkeziFiyat);
        }

        scanner.close();
    }


    private boolean iskontoUygulanacakmi(Stok stok) {

        if (stok.getStokKodu().equals("37")
                || stok.getStokKodu().equals("45833")
                || stok.getStokKodu().equals("82716")
                || stok.getStokKodu().equals("82717")) {
            return false;
        }
        return true;
    }


    private void stokunAktifMerkeziFiyatiniPasifYap(Stok stok, Il il) throws BusinessRuleException {
        Sorgu sorgu = new Sorgu(MerkeziFiyat.class, null, KriterFactory.esit("aktifMi", true));
        sorgu.kriterEkle(KriterFactory.bos("bitisTarihi"));
        sorgu.kriterEkle(KriterFactory.esit("stok", stok));
        sorgu.kriterEkle(KriterFactory.esit("gecerliIl", il));
        if (sorgula(sorgu).size() > 0) {
            List<MerkeziFiyat> mf = sorgula(sorgu);
            for (MerkeziFiyat merkeziFiyat : mf) {
                flush();
                merkeziFiyat.setAktifMi(false);
                merkeziFiyat.setBitisTarihi(getSimdikiTarih());
                kaydet(merkeziFiyat);
            }
        }
    }

    //merkezi fiyatta çalisan kod kismi
    public void merkeziFiyatKontrol1(KoopSatisFisHareket satisFisHareket, MuhatapTip muhatapTip, KoopSatisOdemeSekli odemeSekli, Tarih tarih) throws BusinessRuleException {
        Kurum kurum = yukle(Kurum.class, satisFisHareket.getStok().getKurum().getId());
        MerkeziFiyat merkeziFiyat = getMerkeziFiyat(satisFisHareket.getStok().getStok(), kurum);
        BigDecimal yeniSatisFiyati = satisFisHareket.getYeniSatisFiyat();
        BigDecimal satisFiyati = satisFisHareket.getSatisFiyat().getFiyat();

        if (muhatapTip.equals(MuhatapTip.ORTAK_DISI) || muhatapTip.equals(MuhatapTip.ORTAK_ICI)) {
            if (odemeSekli.equals(KoopSatisOdemeSekli.NAKIT)
                    || odemeSekli.equals(KoopSatisOdemeSekli.MAHSUBEN_ODEMELI)
                    || odemeSekli.equals(KoopSatisOdemeSekli.TESKILAT_ICI_MAHSUBEN)) {
                if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                    if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, merkeziFiyat.getFiyat())) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, merkeziFiyat.getFiyat().toString());
                    }

//                    if (EkoopUtils.isKucuk(yeniSatisFiyati, merkeziFiyat.getTabanFiyat())) {
//                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, merkeziFiyat.getTabanFiyat().toString());
//                    }

                } else {
                    if (!EkoopUtils.isKucukEsit(satisFiyati, merkeziFiyat.getFiyat())) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, merkeziFiyat.getFiyat().toString());
                    }

//                    if (EkoopUtils.isKucuk(satisFiyati, merkeziFiyat.getTabanFiyat())) {
//                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, merkeziFiyat.getTabanFiyat().toString());
//                    }

                }

            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TAKSITLI)) {
                BigDecimal mfVeresiyeOran = getParametre("MERKEZI_FIYATLI_STOKLARDA_KREDI_KARTI_TAKSITLI_SATIS_FINANS_ORANI", true).getBigDecimalDeger();
                BigDecimal maxVeresiyeMFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), mfVeresiyeOran);
                maxVeresiyeMFiyat = merkeziFiyat.getFiyat().add(maxVeresiyeMFiyat);

                if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                    if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, maxVeresiyeMFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxVeresiyeMFiyat.toString());
                    }
                } else {
                    if (!EkoopUtils.isKucukEsit(satisFiyati, maxVeresiyeMFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxVeresiyeMFiyat.toString());
                    }
                }


            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TEK_CEKIM)) {
                BigDecimal mfVeresiyeOran = getParametre("MERKEZI_FIYATLI_STOKLARDA_KREDI_KARTI_TEK_CEKIM_SATIS_FINANS_ORANI", true).getBigDecimalDeger();
                BigDecimal maxVeresiyeMFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), mfVeresiyeOran);
                maxVeresiyeMFiyat = merkeziFiyat.getFiyat().add(maxVeresiyeMFiyat);

                if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                    if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, maxVeresiyeMFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxVeresiyeMFiyat.toString());
                    }
                } else {
                    if (!EkoopUtils.isKucukEsit(satisFiyati, maxVeresiyeMFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxVeresiyeMFiyat.toString());
                    }
                }


            }


            if (odemeSekli.equals(KoopSatisOdemeSekli.VERESIYE)) {
                BigDecimal mfVeresiyeOran = getParametre("MERKEZI_FIYATLI_STOKLARDA_ORTAK_ICI_VERESIYE_SATIS_FINANS_ORANI", true).getBigDecimalDeger();
                BigDecimal maxVeresiyeMFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), mfVeresiyeOran);
                maxVeresiyeMFiyat = merkeziFiyat.getFiyat().add(maxVeresiyeMFiyat);

                if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                    if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, maxVeresiyeMFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxVeresiyeMFiyat.toString());
                    }
                } else {
                    if (!EkoopUtils.isKucukEsit(satisFiyati, maxVeresiyeMFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxVeresiyeMFiyat.toString());
                    }
                }


            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.VADELI)) {
                BigDecimal mfVadeliOran = getParametre("MERKEZI_FIYATLI_STOKLARDA_VADELI_ORTAK_DISI_SATIS_FINANS_ORANI", true).getBigDecimalDeger();
                BigDecimal maxVadeliMFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), mfVadeliOran);
                maxVadeliMFiyat = merkeziFiyat.getFiyat().add(maxVadeliMFiyat);

                if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                    if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, maxVadeliMFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxVadeliMFiyat.toString());
                    }
                } else {
                    if (!EkoopUtils.isKucukEsit(satisFiyati, maxVadeliMFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxVadeliMFiyat.toString());
                    }
                }
            }
        }


    }

    //kredi kısmı tama olmadığından ve yeni istekler geldiğinden geri çekilen kod
    public void merkeziFiyatKontrol(KoopSatisFisHareket satisFisHareket, MuhatapTip muhatapTip, KoopSatisOdemeSekli odemeSekli, Tarih tarih) throws BusinessRuleException {
        Kurum kurum = yukle(Kurum.class, satisFisHareket.getStok().getKurum().getId());
        KoopSatisFis koopSatisFis = (KoopSatisFis) satisFisHareket.getStokFis();
        MerkeziFiyat merkeziFiyat = null;

        Tarih simdikiSaatsizTarih = getSimdikiTarih().getSaatsizTarih();
        if (tarih.getSaatsizTarih().esit(simdikiSaatsizTarih)) {
            merkeziFiyat = getMerkeziFiyat(satisFisHareket.getStok().getStok(), kurum);
        } else if (simdikiSaatsizTarih.after(tarih)) { //Geriye dönük senet düzenleme için
            merkeziFiyat = getMerkeziFiyatTarihli(satisFisHareket.getStok().getStok(), kurum, tarih, satisFisHareket.getYeniSatisFiyat(), satisFisHareket.getSatisFiyat().getFiyat());
        }

        BigDecimal yeniSatisFiyati = satisFisHareket.getYeniSatisFiyat();
        BigDecimal satisFiyati = satisFisHareket.getSatisFiyat().getFiyat();

        if (muhatapTip.equals(MuhatapTip.ORTAK_DISI) || muhatapTip.equals(MuhatapTip.ORTAK_ICI)) {
            if (odemeSekli.equals(KoopSatisOdemeSekli.NAKIT )
                    || odemeSekli.equals(KoopSatisOdemeSekli.MAHSUBEN_ODEMELI)
                    || odemeSekli.equals(KoopSatisOdemeSekli.TESKILAT_ICI_MAHSUBEN)
                    || odemeSekli.equals(KoopSatisOdemeSekli.VERESIYE)) {

                if (satisFisHareket.getZararinaSatisYntKrl() != null) {
                    return;
                }

                if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                    if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, merkeziFiyat.getFiyat()) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, merkeziFiyat.getFiyat().toString());
                    }

                    if (EkoopUtils.isKucuk(yeniSatisFiyati, merkeziFiyat.getTabanFiyat()) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, merkeziFiyat.getTabanFiyat().toString());
                    }

                } else {
                    if (!EkoopUtils.isKucukEsit(satisFiyati, merkeziFiyat.getFiyat()) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, merkeziFiyat.getFiyat().toString());
                    }

                    if (EkoopUtils.isKucuk(satisFiyati, merkeziFiyat.getTabanFiyat()) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, merkeziFiyat.getTabanFiyat().toString());
                    }

                }

            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TEK_CEKIM)) {

                BigDecimal minTabanFiyat = BigDecimal.ZERO;
                BigDecimal maxTavanFiyat = BigDecimal.ZERO;
                if (koopSatisFis.getBanka().isZiraatBnakasi()) {
                    BigDecimal ziraatFinOrani = getParametre("ZIRAAT_BANKASI_FINANS_ORANI", true).getBigDecimalDeger();
                    minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), ziraatFinOrani);
                    minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
                    maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), ziraatFinOrani);
                    maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());
                } else if (koopSatisFis.getBanka().isZiraatBasakKart()) {
                    BigDecimal ziraatFinOrani = getParametre("ZIRAAT_BANKASI_BASAK_KRD_KART_FINANS_ORANI", true).getBigDecimalDeger();
                    minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), ziraatFinOrani);
                    minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
                    maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), ziraatFinOrani);
                    maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());
                } else {
                    BigDecimal digFinOrani = getParametre("DIGER_BANKA_FINANS_ORANI", true).getBigDecimalDeger();
                    minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), digFinOrani);
                    minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
                    maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), digFinOrani);
                    maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());

                }

                if (satisFisHareket.getZararinaSatisYntKrl() != null) {
                    if (EkoopUtils.isBuyuk(satisFisHareket.getMiktar(), satisFisHareket.getZararinaSatisYntKrl().getMiktar())) {
                        throw new BusinessRuleException(StokHataKodu.YNT_KRL_BELIRTILEN_MIKTARI_ASAMAZSINIZ);
                    }

                }

                BigDecimal satisFiyat = EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO) ? yeniSatisFiyati : satisFiyati;

                if (satisFisHareket.getZararinaSatisYntKrl() != null && !EkoopUtils.isBuyuk(satisFiyat, maxTavanFiyat)) {
                    return;
                }

                if (satisFisHareket.getZararinaSatisYntKrl() == null) {
                    vadeliZararinaSatisYonetimKararKrediKart(maxTavanFiyat, minTabanFiyat, yeniSatisFiyati, satisFiyati, satisFisHareket.getStok());
                }


                if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                    if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, maxTavanFiyat) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxTavanFiyat.toString());
                    }

                    if (EkoopUtils.isKucuk(yeniSatisFiyati, minTabanFiyat) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, minTabanFiyat.toString());
                    }

                } else {
                    if (!EkoopUtils.isKucukEsit(satisFiyati, maxTavanFiyat) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxTavanFiyat.toString());
                    }

                    if (EkoopUtils.isKucuk(satisFiyati, minTabanFiyat) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, minTabanFiyat.toString());
                    }
                }
            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TAKSITLI)) {
                if (satisFisHareket.getZararinaSatisYntKrl() != null) {
                    return;
                }
                int taksitSayisi = koopSatisFis.getKrediKartiTaksitSayisi();
                BigDecimal minTabanFiyat = BigDecimal.ZERO;
                BigDecimal maxTavanFiyat = BigDecimal.ZERO;
                String parametreAdi = "ZIRAAT_BANKASI_KRD_KART_TAKSIT" + taksitSayisi + "_FINANS_ORANI";
                BigDecimal ziraatFinOrani = getParametre(parametreAdi, true).getBigDecimalDeger();
                minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), ziraatFinOrani);
                minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
                maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), ziraatFinOrani);
                maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());

                if (satisFisHareket.getZararinaSatisYntKrl() != null) {
                    if (EkoopUtils.isBuyuk(satisFisHareket.getMiktar(), satisFisHareket.getZararinaSatisYntKrl().getMiktar())) {
                        throw new BusinessRuleException(StokHataKodu.YNT_KRL_BELIRTILEN_MIKTARI_ASAMAZSINIZ);
                    }

                }

                BigDecimal satisFiyat = EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO) ? yeniSatisFiyati : satisFiyati;

                if (satisFisHareket.getZararinaSatisYntKrl() != null && !EkoopUtils.isBuyuk(satisFiyat, maxTavanFiyat)) {
                    return;
                }

                if (satisFisHareket.getZararinaSatisYntKrl() == null) {
                    vadeliZararinaSatisYonetimKararKrediKart(maxTavanFiyat, minTabanFiyat, yeniSatisFiyati, satisFiyati, satisFisHareket.getStok());
                }

                if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                    if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, maxTavanFiyat) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxTavanFiyat.toString());
                    }

                    if (EkoopUtils.isKucuk(yeniSatisFiyati, minTabanFiyat) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, minTabanFiyat.toString());
                    }

                } else {
                    if (!EkoopUtils.isKucukEsit(satisFiyati, maxTavanFiyat) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxTavanFiyat.toString());
                    }

                    if (EkoopUtils.isKucuk(satisFiyati, minTabanFiyat) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, minTabanFiyat.toString());
                    }
                }
            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.VADELI) ) {
                int vadeGunSayisi = koopSatisFis.getVeresiyeVade();
                BigDecimal minTabanFiyat = BigDecimal.ZERO;
                BigDecimal maxTavanFiyat = BigDecimal.ZERO;
                BigDecimal yillikOran = getParametre("VADELI_SATIS_YEM_GRUBU_YILLIK_ORANI", true).getBigDecimalDeger();
                BigDecimal vadeGunOrani = new BigDecimal(vadeGunSayisi + "").divide(new BigDecimal("30"), BigDecimal.ROUND_UP);

                minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), yillikOran);
                minTabanFiyat = EkoopUtils.bolVeYuvarla(minTabanFiyat, new BigDecimal("12"), 2);
                minTabanFiyat = minTabanFiyat.multiply(vadeGunOrani);
                minTabanFiyat = EkoopUtils.yuvarla(minTabanFiyat.add(merkeziFiyat.getTabanFiyat()), 2);

                maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), yillikOran);
                maxTavanFiyat = EkoopUtils.bolVeYuvarla(maxTavanFiyat, new BigDecimal("12"), 2);
                maxTavanFiyat = maxTavanFiyat.multiply(vadeGunOrani);
                maxTavanFiyat = EkoopUtils.yuvarla(maxTavanFiyat.add(merkeziFiyat.getFiyat()), 2);
                if (satisFisHareket.getZararinaSatisYntKrl() != null) {
                    if (EkoopUtils.isBuyuk(satisFisHareket.getMiktar(), satisFisHareket.getZararinaSatisYntKrl().getMiktar())) {
                        throw new BusinessRuleException(StokHataKodu.YNT_KRL_BELIRTILEN_MIKTARI_ASAMAZSINIZ);
                    }

                }

                BigDecimal satisFiyat = EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO) ? yeniSatisFiyati : satisFiyati;

                if (satisFisHareket.getZararinaSatisYntKrl() != null && !EkoopUtils.isBuyuk(satisFiyat, maxTavanFiyat)) {
                    return;
                }

                if (satisFisHareket.getZararinaSatisYntKrl() == null) {
                    vadeliZararinaSatisYonetimKararVadeliVeresiye(maxTavanFiyat, minTabanFiyat, yeniSatisFiyati, satisFiyati, satisFisHareket.getStok());
                }


                if (EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO)) {
                    if (!EkoopUtils.isKucukEsit(yeniSatisFiyati, maxTavanFiyat) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {

                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxTavanFiyat.toString());
                    }

                    if (EkoopUtils.isKucuk(yeniSatisFiyati, minTabanFiyat) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, minTabanFiyat.toString());
                    }

                } else {
                    if (!EkoopUtils.isKucukEsit(satisFiyati, maxTavanFiyat) && merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {

                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, maxTavanFiyat.toString());
                    }

                    if (EkoopUtils.isKucuk(satisFiyati, minTabanFiyat) && merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                        throw BusinessRuleException.olustur(StokHataKodu.MERKEZI_TABAN_FIYATTAN_AZ_FIYAT_GIREMEZSINIZ, minTabanFiyat.toString());
                    }
                }


            }

        }
    }

    private void vadeliZararinaSatisYonetimKararKrediKart(BigDecimal tavanFiyat, BigDecimal tabanFiyat, BigDecimal yeniSatisFiyati, BigDecimal satisFiyati, KurumStok stok) throws BusinessRuleException {
        BigDecimal satisFiyat = EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO) ? yeniSatisFiyati : satisFiyati;
        BigDecimal ortlamaMaliyet = getOrtalamaMaliyet(stok);

        if (EkoopUtils.isBuyuk(ortlamaMaliyet, tavanFiyat) && EkoopUtils.isBuyuk(satisFiyat, tabanFiyat)) {
            return;
        } else {
            if (EkoopUtils.isBuyuk(ortlamaMaliyet, satisFiyat)) {
                if (EkoopUtils.isKucuk(satisFiyat, ortlamaMaliyet) && EkoopUtils.isBuyuk(satisFiyat, tabanFiyat)) {
                    throw new BusinessRuleException(StokHataKodu.ZARARINA_SATIS_YONETIM_KARARI_GEREKIR);
                }

                if (EkoopUtils.isKucuk(satisFiyat, tabanFiyat) && EkoopUtils.isKucuk(satisFiyat, ortlamaMaliyet)) {
                    throw new BusinessRuleException(StokHataKodu.ZARARINA_SATIS_YONETIM_KARARI_GEREKIR);
                }

            }

        }


    }

    private void vadeliZararinaSatisYonetimKararVadeliVeresiye(BigDecimal tavanFiyat, BigDecimal tabanFiyat, BigDecimal yeniSatisFiyati, BigDecimal satisFiyati, KurumStok stok) throws BusinessRuleException {
        BigDecimal satisFiyat = EkoopUtils.isBuyuk(yeniSatisFiyati, BigDecimal.ZERO) ? yeniSatisFiyati : satisFiyati;
        BigDecimal ortlamaMaliyet = getOrtalamaMaliyet(stok);

        if (EkoopUtils.isBuyuk(ortlamaMaliyet, tavanFiyat) && EkoopUtils.isBuyuk(satisFiyat, tabanFiyat) && EkoopUtils.isKucuk(satisFiyat, tavanFiyat)) {
            return;
        } else {
            if (EkoopUtils.isBuyuk(ortlamaMaliyet, satisFiyat)) {
                if (EkoopUtils.isKucuk(satisFiyat, ortlamaMaliyet) && EkoopUtils.isBuyuk(satisFiyat, tabanFiyat)) {
                    throw new BusinessRuleException(StokHataKodu.ZARARINA_SATIS_YONETIM_KARARI_GEREKIR);
                }

                if (EkoopUtils.isKucuk(satisFiyat, tabanFiyat) && EkoopUtils.isKucuk(satisFiyat, ortlamaMaliyet)) {
                    throw new BusinessRuleException(StokHataKodu.ZARARINA_SATIS_YONETIM_KARARI_GEREKIR);
                }

            }

        }


    }


    public void kooperatifYemTavanFiyatKontrol(KurumStok kurumstok, BigDecimal fiyat, MuhatapTip muhatapTip, KoopSatisOdemeSekli odemeSekli, KoopSatisFis koopSatisFis, Tarih tarih) throws BusinessRuleException {
        if (!muhatapTip.equals(MuhatapTip.TESKILAT_ICI)) {


            int vadeGunSayisi;
            BigDecimal yillikOran = getParametre("VADELI_SATIS_YEM_GRUBU_YILLIK_ORANI", true).getBigDecimalDeger();
            BigDecimal vadeGunOrani = BigDecimal.ZERO;
            if (null != koopSatisFis.getVeresiyeVade()) {
                vadeGunSayisi = koopSatisFis.getVeresiyeVade();
                vadeGunOrani = new BigDecimal(vadeGunSayisi + "").divide(new BigDecimal("30"), BigDecimal.ROUND_UP);
            }
            if (odemeSekli.equals(KoopSatisOdemeSekli.NAKIT) || odemeSekli.equals(KoopSatisOdemeSekli.VERESIYE)) {


                BigDecimal olmasiGerekenFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);

                BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);

                if( koopSatisFis.getKaynak().equals(StokFisKaynak.ORTAK_KREDI)){




                    if (null != tarih && tarih.getSaatsizTarih().before( new Tarih(25,12, 2018))) {
                        olmasiGerekenFiyat = yemTavanFiyatKrediGecmisTarih(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok, tarih);

                        if (EkoopUtils.isBuyuk(fiyat, olmasiGerekenFiyat)) {
                            throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, olmasiGerekenFiyat.toString());
                        }


                    }else{
                        olmasiGerekenMinTabanFiyat = yemTavanFiyatKrediGecmisTarih(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok, tarih);

                        if (EkoopUtils.isKucuk(fiyat, olmasiGerekenMinTabanFiyat)) {
                            throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString(), olmasiGerekenFiyat.toString());
                        }
                    }


                }else{
                    if (EkoopUtils.isKucuk(fiyat, olmasiGerekenMinTabanFiyat)) {
//                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString(), olmasiGerekenFiyat.toString());
                        throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString(), olmasiGerekenFiyat.toString());
                    }

                    if (EkoopUtils.isBuyuk(fiyat, olmasiGerekenFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, olmasiGerekenFiyat.toString());
                    }
                }

            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.MAHSUBEN_ODEMELI)) {
                BigDecimal olmasiGerekenFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);

                BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);


                if (EkoopUtils.isKucuk(fiyat, olmasiGerekenMinTabanFiyat)) {
//                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_VE_TABAN_FIYAT_ARALIGINDA_ISLEM_FIYAT_GIRMELISINIZ, olmasiGerekenMinTabanFiyat.toString(), olmasiGerekenFiyat.toString());
                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString(), olmasiGerekenFiyat.toString());
                }

                if (EkoopUtils.isBuyuk(fiyat, olmasiGerekenFiyat)) {
                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, olmasiGerekenFiyat.toString());
                }
            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TEK_CEKIM)) {
                if (koopSatisFis.getBanka().isZiraatBnakasi()) {
                    BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                    BigDecimal ziraatBankasiFinansOrani = getParametre("ZIRAAT_BANKASI_FINANS_ORANI", true).getBigDecimalDeger();
                    BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(ziraatBankasiFinansOrani);
                    olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);

                    BigDecimal olmasiGerekenTabanFiyat = getOrtalamaMaliyet(kurumstok).add(getOrtalamaMaliyet(kurumstok).multiply(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true).getBigDecimalDeger()));
                    olmasiGerekenTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenTabanFiyat.add(olmasiGerekenTabanFiyat.multiply(ziraatBankasiFinansOrani)), 2);

                    if (EkoopUtils.isKucuk(fiyat, olmasiGerekenTabanFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_VE_TABAN_FIYAT_ARALIGINDA_ISLEM_FIYAT_GIRMELISINIZ, olmasiGerekenTabanFiyat.toString(), olmasiGerekenMaxTavanFiyat.toString());
//                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString());
                    }

                    if (EkoopUtils.isBuyuk(fiyat, olmasiGerekenMaxTavanFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, olmasiGerekenMaxTavanFiyat.toString());
                    }
                } else if (koopSatisFis.getBanka().isZiraatBasakKart()) {
                    BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                    BigDecimal ziraatBankasiFinansOrani = getParametre("ZIRAAT_BANKASI_BASAK_KRD_KART_FINANS_ORANI", true).getBigDecimalDeger();
                    BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(ziraatBankasiFinansOrani);
                    olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);

                    BigDecimal olmasiGerekenTabanFiyat = getOrtalamaMaliyet(kurumstok).add(getOrtalamaMaliyet(kurumstok).multiply(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true).getBigDecimalDeger()));
                    olmasiGerekenTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenTabanFiyat.add(olmasiGerekenTabanFiyat.multiply(ziraatBankasiFinansOrani)), 2);

                    if (EkoopUtils.isKucuk(fiyat, olmasiGerekenTabanFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_VE_TABAN_FIYAT_ARALIGINDA_ISLEM_FIYAT_GIRMELISINIZ, olmasiGerekenTabanFiyat.toString(), olmasiGerekenMaxTavanFiyat.toString());
//                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString());
                    }

                    if (EkoopUtils.isBuyuk(fiyat, olmasiGerekenMaxTavanFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, olmasiGerekenMaxTavanFiyat.toString());
                    }
                } else {
                    BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                    BigDecimal ziraatBankasiFinansOrani = getParametre("DIGER_BANKA_FINANS_ORANI", true).getBigDecimalDeger();
                    BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(ziraatBankasiFinansOrani);
                    olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);

                    BigDecimal olmasiGerekenTabanFiyat = getOrtalamaMaliyet(kurumstok).add(getOrtalamaMaliyet(kurumstok).multiply(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true).getBigDecimalDeger()));
                    olmasiGerekenTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenTabanFiyat.add(olmasiGerekenTabanFiyat.multiply(ziraatBankasiFinansOrani)), 2);

                    if (EkoopUtils.isKucuk(fiyat, olmasiGerekenTabanFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_VE_TABAN_FIYAT_ARALIGINDA_ISLEM_FIYAT_GIRMELISINIZ, olmasiGerekenTabanFiyat.toString(), olmasiGerekenMaxTavanFiyat.toString());
//                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString());
                    }

                    if (EkoopUtils.isBuyuk(fiyat, olmasiGerekenMaxTavanFiyat)) {
                        throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, olmasiGerekenMaxTavanFiyat.toString());
                    }
                }

            }


            if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TAKSITLI)) {
                int taksitSayisi = koopSatisFis.getKrediKartiTaksitSayisi();

                BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                String parametreAdi = "ZIRAAT_BANKASI_KRD_KART_TAKSIT" + taksitSayisi + "_FINANS_ORANI";
                BigDecimal ziraatBankasiFinansOrani = getParametre(parametreAdi, true).getBigDecimalDeger();
                BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(ziraatBankasiFinansOrani);
                olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);

//                374736 nolu taleple kaldirildi

//                BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);
//                BigDecimal olmasiGerekenTabanFiyat = olmasiGerekenMinTabanFiyat.multiply(ziraatBankasiFinansOrani);
//                olmasiGerekenMinTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenMinTabanFiyat.add(olmasiGerekenTabanFiyat), 2);
//
//                BigDecimal olmasiGerekenTabanFiyat = getOrtalamaMaliyet(kurumstok).multiply(ziraatBankasiFinansOrani);
                BigDecimal olmasiGerekenTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);
//                 olmasiGerekenTabanFiyat = olmasiGerekenTabanFiyat.multiply(ziraatBankasiFinansOrani);
                olmasiGerekenTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenTabanFiyat.add(olmasiGerekenTabanFiyat.multiply(ziraatBankasiFinansOrani)), 2);


                if (EkoopUtils.isBuyuk(fiyat, olmasiGerekenMaxTavanFiyat)) {
                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, olmasiGerekenMaxTavanFiyat.toString());
//                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_VE_TABAN_FIYAT_ARALIGINDA_ISLEM_FIYAT_GIRMELISINIZ, olmasiGerekenTabanFiyat.toString(), olmasiGerekenMaxTavanFiyat.toString());
                }


                if (EkoopUtils.isKucuk(fiyat, olmasiGerekenTabanFiyat)) {
                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_VE_TABAN_FIYAT_ARALIGINDA_ISLEM_FIYAT_GIRMELISINIZ, olmasiGerekenTabanFiyat.toString(), olmasiGerekenMaxTavanFiyat.toString());
//                        throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString());
                }


            }


            if (odemeSekli.equals(KoopSatisOdemeSekli.VADELI) ) {


                BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(yillikOran);
                olmasiGerekenTavanFiyat = olmasiGerekenTavanFiyat.multiply(vadeGunOrani);
                olmasiGerekenTavanFiyat = olmasiGerekenTavanFiyat.divide(new BigDecimal("12"), 2, BigDecimal.ROUND_HALF_UP);

                olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);

                BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);


                BigDecimal olmasiGerekenTabanFiyat = EkoopUtils.tutarCarp(olmasiGerekenMinTabanFiyat, yillikOran);
                olmasiGerekenTabanFiyat = olmasiGerekenTabanFiyat.multiply(vadeGunOrani);
                olmasiGerekenTabanFiyat = EkoopUtils.bolVeYuvarla(olmasiGerekenTabanFiyat, new BigDecimal("12"), 2);
                olmasiGerekenMinTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenMinTabanFiyat.add(olmasiGerekenTabanFiyat), 2);


                if (EkoopUtils.isBuyuk(fiyat, olmasiGerekenMaxTavanFiyat)) {
//                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_FIYATTAN_FAZLA_FIYAT_GIREMEZSINIZ, olmasiGerekenMaxTavanFiyat.toString());
                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_VE_TABAN_FIYAT_ARALIGINDA_ISLEM_FIYAT_GIRMELISINIZ, olmasiGerekenMinTabanFiyat.toString(), olmasiGerekenMaxTavanFiyat.toString());
                }

                if (EkoopUtils.isKucuk(fiyat, olmasiGerekenMinTabanFiyat)) {
                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_VE_TABAN_FIYAT_ARALIGINDA_ISLEM_FIYAT_GIRMELISINIZ, olmasiGerekenMinTabanFiyat.toString(), olmasiGerekenMaxTavanFiyat.toString());
//                    throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TABAN_FIYATTAN_KUCUK_FIYAT_GIREMEZSINIZ, olmasiGerekenMinTabanFiyat.toString());
                }


            }
        }

    }

    public List kooperatifYemTavanFiyatKontrolFiyatDondur(KurumStok kurumstok, KoopSatisOdemeSekli odemeSekli, KrediKartiTaksit taksitSayisi, Integer vadeGunSayisi, Banka banka) throws BusinessRuleException {

        try {


            if (odemeSekli.equals(KoopSatisOdemeSekli.NAKIT)) {

                BigDecimal olmasiGerekenFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                BigDecimal olmasiGerekenTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);

                List<BigDecimal> list = new ArrayList<BigDecimal>();
                list.add(olmasiGerekenFiyat);
                list.add( olmasiGerekenTabanFiyat );
                return list;

            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.MAHSUBEN_ODEMELI)) {
                BigDecimal olmasiGerekenFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                BigDecimal olmasiGerekenTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);

                List<BigDecimal> list = new ArrayList<BigDecimal>();
                list.add(olmasiGerekenFiyat);
                list.add( olmasiGerekenTabanFiyat );
                return list;
            }

            if (odemeSekli.equals(KoopSatisOdemeSekli.TARIMSAL_KREDI_KARTI)
                    || (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI) ) ) {

                if (odemeSekli.equals(KoopSatisOdemeSekli.TARIMSAL_KREDI_KARTI) ) {

                    BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                    BigDecimal ziraatBankasiFinansOrani = getParametre("TARIMSAL_KART_FINANS_ORANI", true).getBigDecimalDeger();
                    BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(ziraatBankasiFinansOrani);
                    olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);

                    BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);
                    BigDecimal olmasiGerekenTabanFiyat = olmasiGerekenMinTabanFiyat.multiply(ziraatBankasiFinansOrani);
                    olmasiGerekenMinTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenMinTabanFiyat.add(olmasiGerekenTabanFiyat), 2);

                    List<BigDecimal> list = new ArrayList<BigDecimal>();
                    list.add(olmasiGerekenMaxTavanFiyat);
                    list.add(olmasiGerekenMinTabanFiyat);
                    return list;
                } else if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI) && taksitSayisi.equals( KrediKartiTaksit.TEK_CEKIM)) {

                    BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                    BigDecimal ziraatBankasiFinansOrani = getParametre("TEK_CEKIM_BANKA_FINANS_ORANI", true).getBigDecimalDeger();
                    BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(ziraatBankasiFinansOrani);
                    olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);

                    BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);
                    BigDecimal olmasiGerekenTabanFiyat = olmasiGerekenMinTabanFiyat.multiply(ziraatBankasiFinansOrani);
                    olmasiGerekenMinTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenMinTabanFiyat.add(olmasiGerekenTabanFiyat), 2);

                    List<BigDecimal> list = new ArrayList<BigDecimal>();
                    list.add(olmasiGerekenMaxTavanFiyat);
                    list.add(olmasiGerekenMinTabanFiyat);
                    return list;
                }else if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI) && !taksitSayisi.equals( KrediKartiTaksit.TEK_CEKIM)) {

                    BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                    String parametreAdi = "KREDI_KART_"+ taksitSayisi.getKod()+ "_TAKSIT_FINANS_ORANI";
                    BigDecimal ziraatBankasiFinansOrani = getParametre(parametreAdi, true).getBigDecimalDeger();
                    BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(ziraatBankasiFinansOrani);
                    olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);

//                374736 nolu taleple kaldirildi

                    BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);
                    BigDecimal olmasiGerekenTabanFiyat = olmasiGerekenMinTabanFiyat.multiply(ziraatBankasiFinansOrani);
                    olmasiGerekenMinTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenMinTabanFiyat.add(olmasiGerekenTabanFiyat), 2);
//
//                BigDecimal olmasiGerekenTabanFiyat = getOrtalamaMaliyet(kurumstok).multiply(ziraatBankasiFinansOrani);
//                olmasiGerekenTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenTavanFiyat.add(getOrtalamaMaliyet(kurumstok)), 2);


                    List<BigDecimal> list = new ArrayList<BigDecimal>();
                    list.add(olmasiGerekenMaxTavanFiyat);
                    list.add(olmasiGerekenMinTabanFiyat);
                    return list;
                }

            }

//            if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TAKSITLI)) {
//
////                if (taksitSayisi > 9)
////                    throw new BusinessRuleException("asd");
//
//
//                BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
//                String parametreAdi = "ZIRAAT_BANKASI_KRD_KART_TAKSIT" + taksitSayisi + "_FINANS_ORANI";
//                BigDecimal ziraatBankasiFinansOrani = getParametre(parametreAdi, true).getBigDecimalDeger();
//                BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(ziraatBankasiFinansOrani);
//                olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);
//
////                374736 nolu taleple kaldirildi
//
//                BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);
//                BigDecimal olmasiGerekenTabanFiyat = olmasiGerekenMinTabanFiyat.multiply(ziraatBankasiFinansOrani);
//                olmasiGerekenMinTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenMinTabanFiyat.add(olmasiGerekenTabanFiyat), 2);
////
////                BigDecimal olmasiGerekenTabanFiyat = getOrtalamaMaliyet(kurumstok).multiply(ziraatBankasiFinansOrani);
////                olmasiGerekenTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenTavanFiyat.add(getOrtalamaMaliyet(kurumstok)), 2);
//
//
//                List<BigDecimal> list = new ArrayList<BigDecimal>();
//                list.add(olmasiGerekenMaxTavanFiyat);
//                list.add(olmasiGerekenMinTabanFiyat);
//                return list;
//
//
//            }



            if (odemeSekli.equals(KoopSatisOdemeSekli.VADELI) || odemeSekli.equals(KoopSatisOdemeSekli.VERESIYE)) {

                BigDecimal yillikOran = getParametre("VADELI_SATIS_YEM_GRUBU_YILLIK_ORANI", true).getBigDecimalDeger();
                BigDecimal vadeGunOrani = new BigDecimal(vadeGunSayisi + "").divide(new BigDecimal("30"), BigDecimal.ROUND_UP);
                BigDecimal olmasiGerekenMaxTavanFiyat = yemTavanFiyat(getParametre("PESIN_SATIS_YEM_GRUBU_SABIT_SATIS_ORANI", true), kurumstok);
                BigDecimal olmasiGerekenTavanFiyat = olmasiGerekenMaxTavanFiyat.multiply(yillikOran);
                olmasiGerekenTavanFiyat = olmasiGerekenTavanFiyat.multiply(vadeGunOrani);
                olmasiGerekenTavanFiyat = olmasiGerekenTavanFiyat.divide(new BigDecimal("12"), 2, BigDecimal.ROUND_HALF_UP);

                olmasiGerekenMaxTavanFiyat = EkoopUtils.yuvarla(olmasiGerekenMaxTavanFiyat.add(olmasiGerekenTavanFiyat), 2);

                BigDecimal olmasiGerekenMinTabanFiyat = yemTavanFiyat(getParametre("VADELI_SATIS_YEM_GRUBU_TABAN_ORANI", true), kurumstok);


                BigDecimal olmasiGerekenTabanFiyat = EkoopUtils.tutarCarp(olmasiGerekenMinTabanFiyat, yillikOran);
                olmasiGerekenTabanFiyat = olmasiGerekenTabanFiyat.multiply(vadeGunOrani);
                olmasiGerekenTabanFiyat = EkoopUtils.bolVeYuvarla(olmasiGerekenTabanFiyat, new BigDecimal("12"), 2);
                olmasiGerekenMinTabanFiyat = EkoopUtils.yuvarla(olmasiGerekenMinTabanFiyat.add(olmasiGerekenTabanFiyat), 2);


                List<BigDecimal> list = new ArrayList<BigDecimal>();
                list.add(olmasiGerekenMaxTavanFiyat);
                list.add(olmasiGerekenMinTabanFiyat);
                return list;


            }

        } catch (Exception e) {
            throw BusinessRuleException.olustur(StokHataKodu.YEM_STOKLARINA_TAVAN_VE_TABAN_FIYAT_ARALIGINDA_ISLEM_FIYAT_GIRMELISINIZ);
//
        }

        return null;

    }

    public void gubreBayiFiyatBelgeKaydet(GubreBayiFiyat gubreBayiFiyat, UploadedFile yuklenenEkDosya) throws BusinessRuleException {
        if (yuklenenEkDosya != null) {
            sizeKontrol(yuklenenEkDosya);
            String uploadedFileName = yuklenenEkDosya.getName();
            int yil = gubreBayiFiyat.getTarih().getYil();
            String filename = "";
            List<Integer> nokta = new ArrayList<Integer>();
            List<String> directory = new ArrayList<String>();
            if (!UygulamaTestUtil.isGercekSistem()) {
                directory.add("Test");
            }
            directory.add("Stok");

            for (int i = 0; i < uploadedFileName.length(); i++) {
                if ('.' == uploadedFileName.charAt(i)) {
                    nokta.add(i);
                }
            }

            directory.add("GubreBayiFiyat");
            directory.add(Integer.toString(yil));
            directory.add(Long.toString(gubreBayiFiyat.getKurum().getId()));

            int index = nokta.get(nokta.size() - 1);
            String fileExtension = uploadedFileName.substring(index, uploadedFileName.length());
            commonServis.fileTypeKontrol(fileExtension, yuklenenEkDosya);
            filename = gubreBayiFiyat.getId() + fileExtension;
            if (gubreBayiFiyat.getDosyaAdi() != null || "".equals(gubreBayiFiyat.getDosyaAdi())) {
                getFtpSunucuYoneticisi().dosyaSil(directory, filename);
            }
            getFtpSunucuYoneticisi().dosyaYukle(yuklenenEkDosya, filename, directory);

            gubreBayiFiyat.setDosyaAdi(filename);
            kaydet(gubreBayiFiyat);
        }
    }

    private void sizeKontrol(UploadedFile uploadedFile) throws BusinessRuleException {
        if (uploadedFile.getSize() > 16000000) {
            throw new BusinessRuleException(CommonHataKodu.DOSYA_COK_BUYUK);
        }
    }

    public void stokFisIslemYapilmamisEfaturaMesajKaydet(Kurum kurum) {
        dao.stokFisIslemYapilmamisEfaturaMesajKaydet(kurum);
    }

    public void gubretasOnayliCikisFisiOlustur(Kullanici kullanici, FaturaFis stokFis) throws BusinessRuleException {
        new GubretasOnayliCikisFisiOlustur(this, kullanici, stokFis).yap();
        stokFis.setAmonyumNitratIadeMi(false);
        kaydet(stokFis);
    }

    public String ciroPrimCriticalSection(Kurum kurum, String yilAy) throws BusinessRuleException {
        if (!canEnterStokCriticalSection(kurum, "CiroPrimFisOnayla", yilAy, true)) {

            throw new BusinessRuleException(CommonHataKodu.BU_ISLEM_DAHA_ONCE_YAPILMIS);
        } else
            return "OK";
    }

    public List<StokGrup> stokGrubuAltindakiTumStokGruplari(StokGrup stokGrup) {
        return dao.stokGrubuAltindakiTumStokGruplari(stokGrup);
    }

    public List<Stok> stokGrubuAltindakiVadeliFaizsizTumStoklar(StokGrup stokGrup, Tarih simdikiTarihSaatsizTarih) {
        return dao.stokGrubuAltindakiVadeliFaizsizTumStoklar(stokGrup, simdikiTarihSaatsizTarih);
    }

    public List<StokFis> getCiroPrimFisler(Kurum kurum, TuzelFirmaMusteri firmaMusteri, String sorguYilAy) {
        return dao.getCiroPrimFisler(kurum, firmaMusteri, sorguYilAy);
    }

    public void validateMustahsilMiktarKontrol(MustahsilMakbuzuFis mustahsilMakbuzuFis) throws BusinessRuleException {

        for (MustahsilMakbuzHareket fisHareket : mustahsilMakbuzuFis.getHareketler()) {

            if (dao.mustahsilMiktarKontrol(fisHareket.getStok().getStok().getGrup(), fisHareket.getMiktar()))
                throw BusinessRuleException.olustur(StokHataKodu.MUSTAHSIL_MAKBUZU_MIKTAR_KONTROLU,
                        fisHareket.getStok().getStok().getStokAdi(), " adli stogu kontrol ediniz.");


        }
    }

    @Override
    public List<Hesap> getKurumaAitAcikAltHesaplar(Kurum kurum, Sorgu sorgu, int uzunluk) {
        return muhasebeModulServis.getKurumaAitAcikAltHesaplar(kurum, sorgu, uzunluk);
    }

    @Override
    public String ciroPrimFarkliFirmaKDVHesapla(FirmaMusteri firmaMusteri, Hesap hesap, Kullanici aktifKullanici, BigDecimal tutar, String oran) throws BusinessRuleException {
        String KDVHesabi = "";
        BigDecimal kdv;
        if (oran == null || "".equals(oran)) {
            kdv = BigDecimal.ZERO;
        } else {
            kdv = EkoopUtils.bolVeYuvarla(EkoopUtils.tutarCarp(tutar, BigDecimal.valueOf(Long.valueOf(oran))), BigDecimal.valueOf(100), 2);
            if (oran.equals("1")) {
                KDVHesabi = "39120000100001";
            } else if (oran.equals("8")) {
                KDVHesabi = "39120000100002";
            } else if (oran.equals("18")) {
                KDVHesabi = "39120000100007";
            }
        }

        Tarih tarih = getSimdikiTarihSadeceTarih();

        MahsupFis mahsupFis = new MahsupFis(tarih, aktifKullanici.getAktifKurum(), FisKaynak.STOK, firmaMusteri.getIsim() + " CİRO PRİM + KDV");
        muhasebeModulServis.geciciMahsupFisiAc(mahsupFis, aktifKullanici);
        mahsupFis.borcEkle(hesap, tutar.add(kdv), "Ciro Prim Toplam Tutar", null);
        if (EkoopUtils.isBuyuk(kdv, BigDecimal.ZERO)) {
            mahsupFis.alacakEkle(muhasebeModulServis.altHesapBulYoksaAc(KDVHesabi, "KDV HESABI", aktifKullanici.getAktifKurum()), kdv, firmaMusteri.getIsim() + " KDV", null);
        }
        mahsupFis.alacakEkle(muhasebeModulServis.altHesapBulYoksaAc("60290000100001", "CİRO PRİM HESABI", aktifKullanici.getAktifKurum()), tutar, " CİRO PRİM", null);
        muhasebeModulServis.kaliciMahsupFisiAc(mahsupFis);
        kaydet(mahsupFis);
        return mahsupFis.getFisNo();
    }

    public String ciroPrimFarkliFirmaOncekiKDVHesapla(FirmaMusteri firmaMusteri, Hesap hesap, Kullanici aktifKullanici, BigDecimal tutar, String oran) throws BusinessRuleException {
        String KDVHesabi = "";
        BigDecimal kdv;
        if (oran == null || "".equals(oran)) {
            kdv = BigDecimal.ZERO;
        } else {
            kdv = EkoopUtils.bolVeYuvarla(EkoopUtils.tutarCarp(tutar, BigDecimal.valueOf(Long.valueOf(oran))), BigDecimal.valueOf(100), 2);
            if (oran.equals("1")) {
                KDVHesabi = "39120000100001";
            } else if (oran.equals("8")) {
                KDVHesabi = "39120000100002";
            } else if (oran.equals("18")) {
                KDVHesabi = "39120000100007";
            }
        }

        Tarih tarih = getOncekiAyinSonIsgunu();

        MahsupFis mahsupFis = new MahsupFis(tarih, aktifKullanici.getAktifKurum(), FisKaynak.STOK, firmaMusteri.getIsim() + " CİRO PRİM + KDV");
        muhasebeModulServis.geciciMahsupFisiAc(mahsupFis, aktifKullanici);
        mahsupFis.borcEkle(hesap, tutar.add(kdv), "Ciro Prim Toplam Tutar", null);
        if (EkoopUtils.isBuyuk(kdv, BigDecimal.ZERO)) {
            mahsupFis.alacakEkle(muhasebeModulServis.altHesapBulYoksaAc(KDVHesabi, "KDV HESABI", aktifKullanici.getAktifKurum()), kdv, firmaMusteri.getIsim() + " KDV", null);
        }
        mahsupFis.alacakEkle(muhasebeModulServis.altHesapBulYoksaAc("60290000100001", "CİRO PRİM HESABI", aktifKullanici.getAktifKurum()), tutar, " CİRO PRİM", null);
        muhasebeModulServis.kaliciMahsupFisiAc(mahsupFis);
        kaydet(mahsupFis);
        return mahsupFis.getFisNo();
    }


    public Sonuc karekodGeciciFisOlustur(String kurumNo, String faturaNo, Tarih faturaTarihi, String barkod, String lotNo, String seriNo, String skt, String vkn, String kullanici, String aciklama, String karekod, String faturaSeri) throws BusinessRuleException {


        getFaturaFisYoneticisi().karekodGeciciFisOlustur(kurumNo, faturaNo, faturaTarihi, barkod, lotNo, seriNo, skt, vkn, kullanici, aciklama, karekod, faturaSeri);
        return new Sonuc(StokRestServisSonuclar.OK, StokRestServisSonuclar.OK.getText());

    }

    public Sonuc karekodGeciciFaturaOlustur(String kurumNo, String faturaNo, String faturaSeri, String karekod, String faturaTarihi, String barkod, String lotNo, String seriNo, String skt, String vkn, String tip, String kullanici, String aciklama) throws BusinessRuleException {


        getFaturaFisYoneticisi().karekodGeciciFaturaOlustur(kurumNo, faturaNo, faturaSeri, karekod, faturaTarihi, barkod, lotNo, seriNo, skt, vkn, tip, kullanici, aciklama);
        return new Sonuc(StokRestServisSonuclar.OK, StokRestServisSonuclar.OK.getText());

    }

    public void geciciFiseSeriEkle(FaturaFis fis) throws BusinessRuleException {


        for (StokHareket hareket : fis.getHareketler()) {
            Sorgu geciciFaturaSorgu = new Sorgu(StokKarekodFatura.class);

            geciciFaturaSorgu.kriterEkle(KriterFactory.esit("kurumNo", fis.getKurum().getKurumNo()));
            geciciFaturaSorgu.kriterEkle(KriterFactory.esit("faturaNo", fis.getFatura().getFaturaNo()));
            geciciFaturaSorgu.kriterEkle(KriterFactory.esit("faturaTarihi", fis.getFatura().getFaturaTarihi().getSaatsizTarih()));

            List<StokKarekodFatura> geciciFaturaList = sorgula(geciciFaturaSorgu);

            for (StokKarekodFatura karekodFatura : geciciFaturaList) {
                Sorgu geciciSeriSorgu = new Sorgu(StokKarekodSeri.class);
                geciciSeriSorgu.kriterEkle(KriterFactory.esit("stokKarekodFatura", karekodFatura));
                List<StokKarekodSeri> karekodSeriList = sorgula(geciciSeriSorgu);

                for (StokKarekodSeri stokKarekodSeri : karekodSeriList) {
                    //stoktaki gercek harekete seri liste detayı ekliyoruz
                    if (hareket.getStok().getStok().getBarkod().equals(stokKarekodSeri.getBarkod())) {


                        getFaturaFisYoneticisi().karekodGeciciFiseSeriEkle(hareket, stokKarekodSeri.getBarkod(), stokKarekodSeri.getLotNumarasi(), stokKarekodSeri.getSeriNo(), stokKarekodSeri.getSonKullanimTarihi(), stokKarekodSeri.getKarekod());

                        // TODO pasiflestirmeyi test edelim hata olabilir burda

                        karekodFatura.setDurum(StokKarekodFaturaDurum.PASIF);
                        kaydet(karekodFatura);
                    }
                }

            }


        }

//        karekodGeciciFiseSeriEkle( FaturaFisHareket hareket, String barkod,String lotNo,String seriNo,String skt);
//
//        Stok Karekod tablosunu pasif yap işlem bitsin

    }

    public void geciciFiseSatisSeriEkle(KoopSatisFis fis) throws BusinessRuleException {

        if (fis.getHareketler().isEmpty())
            throw new BusinessRuleException("Hareket bulunamadi.");


        for (StokHareket hareket : fis.getHareketler()) {
            Sorgu geciciFaturaSorgu = new Sorgu(StokKarekodFatura.class);

            geciciFaturaSorgu.kriterEkle(KriterFactory.esit("kurumNo", fis.getKurum().getKurumNo()));
            geciciFaturaSorgu.kriterEkle(KriterFactory.esit("faturaNo", fis.getFatura().getFaturaNo()));
            geciciFaturaSorgu.kriterEkle(KriterFactory.esit("faturaTarihi", fis.getFatura().getFaturaTarihi().getSaatsizTarih()));

            List<StokKarekodFatura> geciciFaturaList = sorgula(geciciFaturaSorgu);

            for (StokKarekodFatura karekodFatura : geciciFaturaList) {
                Sorgu geciciSeriSorgu = new Sorgu(StokKarekodSeri.class);
                geciciSeriSorgu.kriterEkle(KriterFactory.esit("stokKarekodFatura", karekodFatura));
                List<StokKarekodSeri> karekodSeriList = sorgula(geciciSeriSorgu);

                for (StokKarekodSeri stokKarekodSeri : karekodSeriList) {
                    //stoktaki gercek harekete seri liste detayı ekliyoruz
                    if (hareket.getStok().getStok().getBarkod().equals(stokKarekodSeri.getBarkod())) {


                        getFaturaFisYoneticisi().karekodGeciciFiseSeriEkle(hareket, stokKarekodSeri.getBarkod(), stokKarekodSeri.getLotNumarasi(), stokKarekodSeri.getSeriNo(), stokKarekodSeri.getSonKullanimTarihi(), stokKarekodSeri.getKarekod());

                        // TODO pasiflestirmeyi test edelim hata olabilir burda

                        karekodFatura.setDurum(StokKarekodFaturaDurum.PASIF);
                        kaydet(karekodFatura);
                    }
                }

            }


        }

//        karekodGeciciFiseSeriEkle( FaturaFisHareket hareket, String barkod,String lotNo,String seriNo,String skt);
//
//        Stok Karekod tablosunu pasif yap işlem bitsin

    }

    public void finansmanMaliyetFisKes(FinansmanMaliyetFis finansmanMaliyetFis, Kullanici kullanici) throws BusinessRuleException {

        getStokFisYonetici().stokFisKes(finansmanMaliyetFis, kullanici);

    }

    public void elusGirisFisKes(ElusGirisFis elusFis, Kullanici kullanici) throws BusinessRuleException {

        getStokFisYonetici().stokFisKes(elusFis, kullanici);

    }

    public void elusCikisFisKes(ElusCikisFis elusFis, Kullanici kullanici) throws BusinessRuleException {

        getStokFisYonetici().stokFisKes(elusFis, kullanici);

    }

    public void finansmanMaliyetFisOnayla(FinansmanMaliyetFis fmFis) throws BusinessRuleException {
        getStokFisYonetici().finansmanMaliyetiFisOnayla(fmFis);
    }

    public BigDecimal finansmanMaliyetFisHesapla(BigDecimal fisTutar, Tarih faizBaslangicTarihi, Tarih faizBitisTarihi, Kurum kurum, BigDecimal finansMaliyetiYuzde) throws BusinessRuleException {

        BigDecimal finansMaliyetToplamTutar = BigDecimal.ZERO;

        BigDecimal finansMaliyetParcaTutar = BigDecimal.ZERO;

        KrediTeskilatFaizOranTipi oranTip = null;
        if (kurum.isBolge())
            oranTip = KrediTeskilatFaizOranTipi.MERKEZ_BOLGE_ORANI;
        else if (kurum.isKooperatif())
            oranTip = KrediTeskilatFaizOranTipi.BOLGE_KOOPERATIF_ORANI;


        Sorgu tumDonemSorgu = new Sorgu(KrediTeskilatFaizOranlari.class);
        tumDonemSorgu.kriterEkle(KriterFactory.kucuk("baslangicTarihi", faizBaslangicTarihi.gunEkle(1)));
        tumDonemSorgu.kriterEkle(KriterFactory.buyuk("bitisTarihi", faizBitisTarihi.gunCikar(1)));

        tumDonemSorgu.kriterEkle(KriterFactory.esit("oranTipi", oranTip));
        List<KrediTeskilatFaizOranlari> tumDonemList = sorgula(tumDonemSorgu);
        if (!tumDonemList.isEmpty()) {

            finansMaliyetParcaTutar = fisTutar.multiply(tumDonemList.get(0).getOran().multiply(BigDecimal.valueOf(faizBitisTarihi.getGunFark(faizBaslangicTarihi.gunCikar(1)))));
            finansMaliyetParcaTutar = finansMaliyetParcaTutar.divide(BigDecimal.valueOf(36000), 4, RoundingMode.HALF_UP);

            finansMaliyetParcaTutar = finansMaliyetParcaTutar.multiply(finansMaliyetiYuzde);
            finansMaliyetParcaTutar = finansMaliyetParcaTutar.divide(new BigDecimal(100));

            finansMaliyetToplamTutar = finansMaliyetToplamTutar.add(finansMaliyetParcaTutar);
        }

        Sorgu yariDonemSorgu1 = new Sorgu(KrediTeskilatFaizOranlari.class);
        yariDonemSorgu1.kriterEkle(KriterFactory.kucuk("baslangicTarihi", faizBaslangicTarihi.gunEkle(1)));
        yariDonemSorgu1.kriterEkle(KriterFactory.kucuk("bitisTarihi", faizBitisTarihi));
        yariDonemSorgu1.kriterEkle(KriterFactory.buyuk("bitisTarihi", faizBaslangicTarihi.gunCikar(1)));
        yariDonemSorgu1.kriterEkle(KriterFactory.esit("oranTipi", oranTip));
        List<KrediTeskilatFaizOranlari> yariDonemList1 = sorgula(yariDonemSorgu1);
        if (!yariDonemList1.isEmpty()) {

            finansMaliyetParcaTutar = finansMaliyetParcaTutar.add(fisTutar.multiply(yariDonemList1.get(0).getOran().multiply(BigDecimal.valueOf(yariDonemList1.get(0).getBitisTarihi().getGunFark(faizBaslangicTarihi.gunCikar(1))))));
            finansMaliyetParcaTutar = finansMaliyetParcaTutar.divide(BigDecimal.valueOf(36000), 4, RoundingMode.HALF_UP);

            finansMaliyetParcaTutar = finansMaliyetParcaTutar.multiply(finansMaliyetiYuzde);
            finansMaliyetParcaTutar = finansMaliyetParcaTutar.divide(new BigDecimal(100));

            finansMaliyetToplamTutar = finansMaliyetToplamTutar.add(finansMaliyetParcaTutar);
        }

        Sorgu yariDonemSorgu2 = new Sorgu(KrediTeskilatFaizOranlari.class);
        yariDonemSorgu2.kriterEkle(KriterFactory.buyuk("baslangicTarihi", faizBaslangicTarihi));
        yariDonemSorgu2.kriterEkle(KriterFactory.buyuk("bitisTarihi", faizBitisTarihi.gunCikar(1)));
        yariDonemSorgu2.kriterEkle(KriterFactory.kucuk("baslangicTarihi", faizBitisTarihi.gunCikar(1)));
        yariDonemSorgu2.kriterEkle(KriterFactory.esit("oranTipi", oranTip));
        List<KrediTeskilatFaizOranlari> yariDonemList2 = sorgula(yariDonemSorgu2);
        if (!yariDonemList2.isEmpty()) {

            finansMaliyetParcaTutar = finansMaliyetParcaTutar.add(fisTutar.multiply(yariDonemList2.get(0).getOran().multiply(BigDecimal.valueOf(faizBitisTarihi.getGunFark(yariDonemList2.get(0).getBaslangicTarihi().gunCikar(1))))));
            finansMaliyetParcaTutar = finansMaliyetParcaTutar.divide(BigDecimal.valueOf(36000), 4, RoundingMode.HALF_UP);

            finansMaliyetParcaTutar = finansMaliyetParcaTutar.multiply(finansMaliyetiYuzde);
            finansMaliyetParcaTutar = finansMaliyetParcaTutar.divide(new BigDecimal(100));

            finansMaliyetToplamTutar = finansMaliyetToplamTutar.add(finansMaliyetParcaTutar);
        }

        Sorgu araDonemSorgu = new Sorgu(KrediTeskilatFaizOranlari.class);
        araDonemSorgu.kriterEkle(KriterFactory.buyuk("baslangicTarihi", faizBaslangicTarihi));
        araDonemSorgu.kriterEkle(KriterFactory.kucuk("bitisTarihi", faizBitisTarihi));
        araDonemSorgu.kriterEkle(KriterFactory.esit("oranTipi", oranTip));
        List<KrediTeskilatFaizOranlari> araDonemList = sorgula(araDonemSorgu);

        for (KrediTeskilatFaizOranlari oran : araDonemList) {


            finansMaliyetParcaTutar = finansMaliyetParcaTutar.add(fisTutar.multiply(oran.getOran().multiply(BigDecimal.valueOf(oran.getBitisTarihi().getGunFark(oran.getBaslangicTarihi().gunCikar(1))))));
            finansMaliyetParcaTutar = finansMaliyetParcaTutar.divide(BigDecimal.valueOf(36000), 4, RoundingMode.HALF_UP);

            finansMaliyetParcaTutar = finansMaliyetParcaTutar.multiply(finansMaliyetiYuzde);
            finansMaliyetParcaTutar = finansMaliyetParcaTutar.divide(new BigDecimal(100));

            finansMaliyetToplamTutar = finansMaliyetToplamTutar.add(finansMaliyetParcaTutar);


        }


        return finansMaliyetToplamTutar;
    }

    public StokIptalFis finansmanMaliyetFisIptal(FinansmanMaliyetFis fmFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().finansmanMaliyetFisIptal(fmFis, kullanici, kesilenFisler);
    }

    public void indirimTutarHesaplaKaydet(KoopSatisFisHareket hareket, KoopSatisFis satisFis) throws BusinessRuleException {

        Sorgu sorguKampanyaKurum = new Sorgu(StokKampanyaKurum.class);
        sorguKampanyaKurum.kriterEkle(KriterFactory.kucuk("stokKampanya.baslangicTarihi", satisFis.getFisTarihi().gunEkle(1)));
        sorguKampanyaKurum.kriterEkle(KriterFactory.buyuk("stokKampanya.bitisTarihi", satisFis.getFisTarihi().gunCikar(1)));
        sorguKampanyaKurum.kriterEkle(KriterFactory.esit("kurum", satisFis.getKurum().getUstKurum()));


        List<StokKampanyaKurum> stokKampanyaList = sorgula(sorguKampanyaKurum);

        if (!stokKampanyaList.isEmpty() && stokKampanyaList.get(0).getStokKampanya().isToplamaUygulansinMi()) {
            BigDecimal toplamMiktar = BigDecimal.ZERO;

            List<KampanyaliStok> kampanyaliStokList = sorgula(new Sorgu(KampanyaliStok.class));


            for (KampanyaliStok kampanyaliStok : kampanyaliStokList) {
                if (kampanyaliStok.getStok().equals(hareket.getStok().getStok()))
                    toplamMiktar = toplamMiktar.add(hareket.getMiktar());
            }

            for (StokHareket hareket2 : satisFis.getHareketler()) {


                for (KampanyaliStok kampanyaliStok : kampanyaliStokList) {
                    if (kampanyaliStok.getStok().equals(hareket2.getStok().getStok()))
                        toplamMiktar = toplamMiktar.add(hareket2.getMiktar());
                }

            }


            if (!stokKampanyaList.isEmpty()) {
                if (toplamMiktar.compareTo(stokKampanyaList.get(0).getMiktar()) >= 0) {

                    BigDecimal yapilacakToplamIndirim = stokKampanyaList.get(0).getIskontoTutar().multiply(toplamMiktar.divide(new BigDecimal(50), 0, RoundingMode.DOWN));
                    hareket.setToplamIndirimTutar(yapilacakToplamIndirim.multiply(hareket.getMiktar().divide(toplamMiktar, 4, RoundingMode.HALF_UP)));
//                    hareket.setMusterininOdeyecegiTutar( hareket.getMusterininOdeyecegiTutar().subtract(
//                            yapilacakToplamIndirim.multiply( hareket.getMiktar().divide(toplamMiktar ,4,RoundingMode.HALF_UP))));


                    for (KoopSatisFisHareket hareket2 : satisFis.getHareketler()) {

                        for (KampanyaliStok kampanyaliStok : kampanyaliStokList) {
                            if (kampanyaliStok.getStok().equals(hareket2.getStok().getStok())) {

                                //TODO indirim tutarini setlemek yetmez odeyecegi tutar falan komple hesaplanacak.

                                hareket2.setToplamIndirimTutar(yapilacakToplamIndirim.multiply(hareket2.getMiktar().divide(toplamMiktar, 4, RoundingMode.HALF_UP)));

                                SatisFisYoneticisi satisFisYoneticisi = new SatisFisYoneticisi(this);
                                satisFisYoneticisi.koopTutarHesapla(hareket2, satisFis);

                                kaydet(hareket2);
                            }

                        }

                    }
                } else {
                    hareket.setToplamIndirimTutar(BigDecimal.ZERO);

                    for (KoopSatisFisHareket hareket2 : satisFis.getHareketler()) {

                        for (KampanyaliStok kampanyaliStok : kampanyaliStokList) {
                            if (kampanyaliStok.getStok().equals(hareket2.getStok().getStok())) {

                                //TODO indirim tutarini setlemek yetmez odeyecegi tutar falan komple hesaplanacak.


//                                hareket2.setMusterininOdeyecegiTutar( hareket2.getMusterininOdeyecegiTutar().add(
//                                        hareket2.getToplamIndirimTutar()));
                                hareket2.setToplamIndirimTutar(BigDecimal.ZERO);

                                SatisFisYoneticisi satisFisYoneticisi = new SatisFisYoneticisi(this);
                                satisFisYoneticisi.koopTutarHesapla(hareket2, satisFis);

                                kaydet(hareket2);
                            }

                        }

                    }
                }

            }
        }


    }

    public void indirimTutarHesapla(KoopSatisFisHareket hareket, KoopSatisFis satisFis) throws BusinessRuleException {

        Sorgu sorguKampanyaKurum = new Sorgu(StokKampanyaKurum.class);
        sorguKampanyaKurum.kriterEkle(KriterFactory.kucuk("stokKampanya.baslangicTarihi", satisFis.getFisTarihi().gunEkle(1)));
        sorguKampanyaKurum.kriterEkle(KriterFactory.buyuk("stokKampanya.bitisTarihi", satisFis.getFisTarihi().gunCikar(1)));
        sorguKampanyaKurum.kriterEkle(KriterFactory.esit("kurum", satisFis.getKurum().getUstKurum()));


        List<StokKampanyaKurum> stokKampanyaList = sorgula(sorguKampanyaKurum);

        if (!stokKampanyaList.isEmpty() && stokKampanyaList.get(0).getStokKampanya().isToplamaUygulansinMi()) {
            BigDecimal toplamMiktar = BigDecimal.ZERO;

            List<KampanyaliStok> kampanyaliStokList = sorgula(new Sorgu(KampanyaliStok.class));


            for (KampanyaliStok kampanyaliStok : kampanyaliStokList) {
                if (kampanyaliStok.getStok().equals(hareket.getStok().getStok()))
                    toplamMiktar = toplamMiktar.add(hareket.getMiktar());
            }

            for (StokHareket hareket2 : satisFis.getHareketler()) {


                for (KampanyaliStok kampanyaliStok : kampanyaliStokList) {
                    if (kampanyaliStok.getStok().equals(hareket2.getStok().getStok()))
                        toplamMiktar = toplamMiktar.add(hareket2.getMiktar());
                }

            }

            if (!stokKampanyaList.isEmpty()) {
                if (toplamMiktar.compareTo(stokKampanyaList.get(0).getMiktar()) >= 0) {

                    BigDecimal yapilacakToplamIndirim = stokKampanyaList.get(0).getIskontoTutar().multiply(toplamMiktar.divide(new BigDecimal(50), 0, RoundingMode.DOWN));
                    hareket.setToplamIndirimTutar(yapilacakToplamIndirim.multiply(hareket.getMiktar().divide(toplamMiktar, 4, RoundingMode.HALF_UP)));

//                    hareket.setMusterininOdeyecegiTutar( BigDecimal.ZERO.
//                            subtract( yapilacakToplamIndirim.multiply( hareket.getMiktar().divide(toplamMiktar ,4,RoundingMode.HALF_UP) )));

                } else {
                    hareket.setToplamIndirimTutar(BigDecimal.ZERO);
                }

            }
        }

        if (!stokKampanyaList.isEmpty() && !stokKampanyaList.get(0).getStokKampanya().isToplamaUygulansinMi()) {
            BigDecimal toplamMiktar = BigDecimal.ZERO;

            List<KampanyaliStok> kampanyaliStokList = sorgula(new Sorgu(KampanyaliStok.class));


            for (KampanyaliStok kampanyaliStok : kampanyaliStokList) {
                if (kampanyaliStok.getStok().equals(hareket.getStok().getStok()))
                    toplamMiktar = toplamMiktar.add(hareket.getMiktar());
            }

            if (!stokKampanyaList.isEmpty()) {
                if (toplamMiktar.compareTo(stokKampanyaList.get(0).getMiktar()) >= 0) {

                    BigDecimal yapilacakToplamIndirim = stokKampanyaList.get(0).getIskontoTutar().multiply(toplamMiktar.divide(new BigDecimal(50), 0, RoundingMode.DOWN));
                    hareket.setToplamIndirimTutar(yapilacakToplamIndirim);

                } else {
                    hareket.setToplamIndirimTutar(BigDecimal.ZERO);
                }

            }
        }

    }

    public List<LisansliDepo> getLisansliDepo(Sorgu sorgu) {
        return dao.sorgula(sorgu);
    }

    public void elusGirisFisiOnayla(ElusGirisFis egFis) throws BusinessRuleException {
        getStokFisYonetici().elusGirisFisiOnayla(egFis);
    }

    public void elusCikisFisiOnayla(ElusCikisFis egFis) throws BusinessRuleException {
        getStokFisYonetici().elusCikisFisiOnayla(egFis);
    }

    public List<YemSiparis> onlineSiparisListesiGetir(Kurum kurum, Stok stok) {
        return dao.onlineSiparisler(kurum, stok);
    }

    public void elusGirisFisOnayla(ElusGirisFis egFis) throws BusinessRuleException {
        getStokFisYonetici().elusGirisFisiOnayla(egFis);
    }

    public StokIptalFis elusGirisFisIptal(ElusGirisFis egFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().elusGirisFisIptal(egFis, kullanici, kesilenFisler);
    }

    public StokIptalFis elusCikisFisIptal(ElusCikisFis egFis, Kullanici kullanici, ArrayList<StokFis> kesilenFisler) throws BusinessRuleException {
        return getStokFisYonetici().elusCikisFisIptal(egFis, kullanici, kesilenFisler);
    }

    public BigDecimal getOrtalamaMaliyetTarihli(KurumStok stok, Tarih tarih) {
        BigDecimal mevcutMiktar = getMevcutStokMiktariTarihli(stok, tarih);

        if (mevcutMiktar == null || mevcutMiktar.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return EkoopUtils.bolVeYuvarla(getMevcutStokTutariTarihli(stok, tarih), mevcutMiktar, 5);
    }

//    public boolean personelSatisMi(String tcKimlikNo){
//
//        Sorgu personelSorgu = new Sorgu( Personel.class);
////        personelSorgu.kriterEkle();
//        return dao.personelSatisMi( tcKimlikNo );
//    }

    public HashMap merkeziFiyatKontrolFiyatDondur(KurumStok kurumStok, KoopSatisOdemeSekli odemeSekli, KrediKartiTaksit taksitSayisi, Integer veresiyeVadeGunSayisi, Banka banka) throws BusinessRuleException {
        Kurum kurum = yukle(Kurum.class, kurumStok.getKurum().getId());

        MerkeziFiyat merkeziFiyat = null;


        HashMap<String, BigDecimal> hashMapFiyatList = new HashMap<String, BigDecimal>();

        merkeziFiyat = getMerkeziFiyat(kurumStok.getStok(), kurum);


        if (odemeSekli.equals(KoopSatisOdemeSekli.NAKIT)
                || odemeSekli.equals(KoopSatisOdemeSekli.MAHSUBEN_ODEMELI)
                || odemeSekli.equals(KoopSatisOdemeSekli.TESKILAT_ICI_MAHSUBEN)) {

            if (merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                hashMapFiyatList.put("TABAN", merkeziFiyat.getTabanFiyat());
            }

            if (merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                hashMapFiyatList.put("TAVAN", merkeziFiyat.getFiyat());
            }
        }

//
//        if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TEK_CEKIM)) {
//
//            BigDecimal minTabanFiyat = BigDecimal.ZERO;
//            BigDecimal maxTavanFiyat = BigDecimal.ZERO;
//            if (banka.isZiraatBnakasi()) {
//                BigDecimal ziraatFinOrani = getParametre("ZIRAAT_BANKASI_FINANS_ORANI", true).getBigDecimalDeger();
//                minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), ziraatFinOrani);
//                minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
//                maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), ziraatFinOrani);
//                maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());
//            } else if (banka.isZiraatBasakKart()) {
//                BigDecimal ziraatFinOrani = getParametre("ZIRAAT_BANKASI_BASAK_KRD_KART_FINANS_ORANI", true).getBigDecimalDeger();
//                minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), ziraatFinOrani);
//                minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
//                maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), ziraatFinOrani);
//                maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());
//            } else {
//                BigDecimal digFinOrani = getParametre("DIGER_BANKA_FINANS_ORANI", true).getBigDecimalDeger();
//                minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), digFinOrani);
//                minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
//                maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), digFinOrani);
//                maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());
//
//            }
//
//
//            if (merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
//                hashMapFiyatList.put("TABAN", minTabanFiyat);
//            }
//
//            if (merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
//                hashMapFiyatList.put("TAVAN", maxTavanFiyat);
//            }
//        }
//
//        if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI_TAKSITLI)) {
//
//
//            BigDecimal minTabanFiyat = BigDecimal.ZERO;
//            BigDecimal maxTavanFiyat = BigDecimal.ZERO;
//            String parametreAdi = "ZIRAAT_BANKASI_KRD_KART_TAKSIT" + taksitSayisi + "_FINANS_ORANI";
//            BigDecimal ziraatFinOrani = getParametre(parametreAdi, true).getBigDecimalDeger();
//            minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), ziraatFinOrani);
//            minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
//            maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), ziraatFinOrani);
//            maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());
//
//
//            if (merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
//                hashMapFiyatList.put("TABAN", minTabanFiyat);
//            }
//
//            if (merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
//                hashMapFiyatList.put("TAVAN", maxTavanFiyat);
//            }
//        }

        if (odemeSekli.equals(KoopSatisOdemeSekli.KREDI_KARTI)) {


            BigDecimal minTabanFiyat = BigDecimal.ZERO;
            BigDecimal maxTavanFiyat = BigDecimal.ZERO;
            String parametreAdi = null;


            if( null != taksitSayisi){

                if( taksitSayisi.equals( KrediKartiTaksit.TEK_CEKIM)){
                    parametreAdi = "TEK_CEKIM_BANKA_FINANS_ORANI";
                    BigDecimal ziraatFinOrani = getParametre(parametreAdi, true).getBigDecimalDeger();

                    minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), ziraatFinOrani);
                    minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
                    maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), ziraatFinOrani);
                    maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());
                }

                if( !taksitSayisi.equals( KrediKartiTaksit.TEK_CEKIM)){
                    parametreAdi = "KREDI_KART_"+ taksitSayisi.getKod()+"_TAKSIT_FINANS_ORANI";
                    BigDecimal ziraatFinOrani = getParametre(parametreAdi, true).getBigDecimalDeger();

                    minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), ziraatFinOrani);
                    minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
                    maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), ziraatFinOrani);
                    maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());
                }

            }

            if (merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                hashMapFiyatList.put("TABAN", minTabanFiyat);
            }

            if (merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                hashMapFiyatList.put("TAVAN", maxTavanFiyat);
            }
        }

        if (odemeSekli.equals(KoopSatisOdemeSekli.TARIMSAL_KREDI_KARTI)) {


            BigDecimal minTabanFiyat = BigDecimal.ZERO;
            BigDecimal maxTavanFiyat = BigDecimal.ZERO;
            String parametreAdi = "TARIMSAL_KART_FINANS_ORANI";
            BigDecimal finOrani = getParametre(parametreAdi, true).getBigDecimalDeger();
            minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), finOrani);
            minTabanFiyat = minTabanFiyat.add(merkeziFiyat.getTabanFiyat());
            maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), finOrani);
            maxTavanFiyat = maxTavanFiyat.add(merkeziFiyat.getFiyat());


            if (merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                hashMapFiyatList.put("TABAN", minTabanFiyat);
            }

            if (merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                hashMapFiyatList.put("TAVAN", maxTavanFiyat);
            }
        }

        if (odemeSekli.equals(KoopSatisOdemeSekli.VADELI) || odemeSekli.equals(KoopSatisOdemeSekli.VERESIYE)) {

            BigDecimal minTabanFiyat = BigDecimal.ZERO;
            BigDecimal maxTavanFiyat = BigDecimal.ZERO;
            BigDecimal yillikOran = getParametre("VADELI_SATIS_YEM_GRUBU_YILLIK_ORANI", true).getBigDecimalDeger();
            BigDecimal vadeGunOrani = new BigDecimal(veresiyeVadeGunSayisi + "").divide(new BigDecimal("30"), BigDecimal.ROUND_UP);

            minTabanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getTabanFiyat(), yillikOran);
            minTabanFiyat = EkoopUtils.bolVeYuvarla(minTabanFiyat, new BigDecimal("12"), 2);
            minTabanFiyat = minTabanFiyat.multiply(vadeGunOrani);
            minTabanFiyat = EkoopUtils.yuvarla(minTabanFiyat.add(merkeziFiyat.getTabanFiyat()), 2);

            maxTavanFiyat = EkoopUtils.tutarCarp(merkeziFiyat.getFiyat(), yillikOran);
            maxTavanFiyat = EkoopUtils.bolVeYuvarla(maxTavanFiyat, new BigDecimal("12"), 2);
            maxTavanFiyat = maxTavanFiyat.multiply(vadeGunOrani);
            maxTavanFiyat = EkoopUtils.yuvarla(maxTavanFiyat.add(merkeziFiyat.getFiyat()), 2);


            if (merkeziFiyat.getStok().getTabanFiyatUygulanacak()) {
                hashMapFiyatList.put("TABAN", minTabanFiyat);
            }

            if (merkeziFiyat.getStok().getTavanFiyatUygulanacak()) {
                hashMapFiyatList.put("TAVAN", maxTavanFiyat);
            }


        }

        return hashMapFiyatList;
    }


    public List<SelectItem> tabanTavanTalepFiyatDoldurKredi(KurumStok kurumStok, Tarih senetTarihi) {

        List<SelectItem> selectItems = new ArrayList<SelectItem>();

        if (null != kurumStok.getStok().getStokKodu()) {

            BigDecimal satilanMiktar = BigDecimal.ZERO;
            Sorgu tabanTavanTalepSorgu = new Sorgu(TabanTavanKaldirmaTalep.class);
            tabanTavanTalepSorgu.kriterEkle(KriterFactory.esit("kurum.id", kurumStok.getKurum().getId()));
            tabanTavanTalepSorgu.kriterEkle(KriterFactory.esit("stok.stok.stokKodu", kurumStok.getStok().getStokKodu()));
            tabanTavanTalepSorgu.kriterEkle(KriterFactory.esit("odemeSekli", KoopSatisOdemeSekli.KREDILI));
            tabanTavanTalepSorgu.kriterEkle(KriterFactory.esit("onayliMi", true));
            tabanTavanTalepSorgu.kriterEkle(KriterFactory.dolu("onayTarihi"));
            tabanTavanTalepSorgu.kriterEkle(KriterFactory.buyuk("onayTarihi", senetTarihi.gunCikar(3)));
            tabanTavanTalepSorgu.kriterEkle(KriterFactory.kucuk("onayTarihi", senetTarihi.gunEkle(1)));
            tabanTavanTalepSorgu.kriterEkle(KriterFactory.esit("onayTip", TabanTavanKaldirmaOnayTip.KABUL));

            tabanTavanTalepSorgu.kriterEkle(KriterFactory.esit("tip", TabanTavanKaldirmaTip.TABAN));


            tabanTavanTalepSorgu.setSiralamaKriteri(new SiralamaKriteri("onayTarihi", false));

            List<TabanTavanKaldirmaTalep> talepList = sorgula(tabanTavanTalepSorgu);

            //taban icin bir talep gelebilir en fazla
            if (!talepList.isEmpty()) {

                if (getSimdikiTarih().getSaatsizTarih().equals(senetTarihi)) {

                    Sorgu satilanMiktarSorgu = new Sorgu(StokHareket.class);
                    satilanMiktarSorgu.kriterEkle(KriterFactory.notBenzer("stokFis.fisNo", "GE"));
                    satilanMiktarSorgu.kriterEkle(KriterFactory.esit("tabanTavanKaldirmaTalep", talepList.get(0)));
                    satilanMiktarSorgu.kriterEkle(KriterFactory.bos("stokFis.iptalEdenFis"));
                    satilanMiktarSorgu.kriterEkle(KriterFactory.esit("stokFis.kurum", kurumStok.getKurum()));
                    List<StokHareket> satilanMiktarHareketler = sorgula(satilanMiktarSorgu);
                    for (StokHareket hareket : satilanMiktarHareketler) {
                        satilanMiktar = satilanMiktar.add(hareket.getMiktar());
                    }

                    flush();
                    satilanMiktarHareketler = null;

                    selectItems.add(new SelectItem(talepList.get(0), "Talep Tipi: " + commonServis.bundleAl(talepList.get(0).getTip().getEtiket()) + " ## Fiyat " + talepList.get(0).getTalepFiyat()
                            + "  ## Toplam Miktar: " + talepList.get(0).getMiktar() + " ## Kalan Bakiye: " + talepList.get(0).getMiktar().subtract(satilanMiktar) + " ## Geçerlilik Tarihi: " + talepList.get(0).getOnayTarihi().gunEkle(3)));

                } else {
                    for (TabanTavanKaldirmaTalep talep : talepList) {
                        Sorgu satilanMiktarSorgu = new Sorgu(StokHareket.class);
                        satilanMiktarSorgu.kriterEkle(KriterFactory.notBenzer("stokFis.fisNo", "GE"));
                        satilanMiktarSorgu.kriterEkle(KriterFactory.esit("tabanTavanKaldirmaTalep", talep));
                        satilanMiktarSorgu.kriterEkle(KriterFactory.bos("stokFis.iptalEdenFis"));
                        satilanMiktarSorgu.kriterEkle(KriterFactory.esit("stokFis.kurum", kurumStok.getKurum()));
                        List<StokHareket> satilanMiktarHareketler = sorgula(satilanMiktarSorgu);
                        for (StokHareket hareket : satilanMiktarHareketler) {
                            satilanMiktar = satilanMiktar.add(hareket.getMiktar());
                        }

                        flush();
                        satilanMiktarHareketler = null;

                        selectItems.add(new SelectItem(talep, "Talep Tipi: " + commonServis.bundleAl(talep.getTip().getEtiket()) + " ## Fiyat " + talep.getTalepFiyat()
                                + "  ## Toplam Miktar: " + talep.getMiktar() + " ## Kalan Bakiye: " + talep.getMiktar().subtract(satilanMiktar) + " ## Geçerlilik Tarihi: " + talep.getOnayTarihi().gunEkle(3)));
                    }
                }

            }
            tabanTavanTalepSorgu.kriterCikar("tip");

            tabanTavanTalepSorgu.kriterEkle(KriterFactory.esit("tip", TabanTavanKaldirmaTip.TAVAN));

            talepList = sorgula(tabanTavanTalepSorgu);

            satilanMiktar = BigDecimal.ZERO;

            //tavan icin bir kayıt gelebilir en fazla
            if (!talepList.isEmpty()) {

                if (getSimdikiTarih().getSaatsizTarih().equals(senetTarihi)) {


                    Sorgu satilanMiktarSorgu = new Sorgu(StokHareket.class);
                    satilanMiktarSorgu.kriterEkle(KriterFactory.esit("tabanTavanKaldirmaTalep", talepList.get(0)));
                    satilanMiktarSorgu.kriterEkle(KriterFactory.bos("stokFis.iptalEdenFis"));
                    satilanMiktarSorgu.kriterEkle(KriterFactory.esit("stokFis.kurum", kurumStok.getKurum()));
                    List<StokHareket> satilanMiktarHareketler = sorgula(satilanMiktarSorgu);
                    for (StokHareket hareket : satilanMiktarHareketler) {
                        satilanMiktar = satilanMiktar.add(hareket.getMiktar());
                    }

                    flush();
                    satilanMiktarHareketler = null;

                    selectItems.add(new SelectItem(talepList.get(0), "Talep Tipi: " + commonServis.bundleAl(talepList.get(0).getTip().getEtiket()) + " ## Fiyat: " + talepList.get(0).getTalepFiyat()
                            + "  ## Toplam Miktar: " + talepList.get(0).getMiktar() + " ## Kalan Bakiye: " + talepList.get(0).getMiktar().subtract(satilanMiktar) + " ## Geçerlilik Tarihi: " + talepList.get(0).getOnayTarihi().gunEkle(3)));


                } else {
                    for (TabanTavanKaldirmaTalep talep : talepList) {


                        Sorgu satilanMiktarSorgu = new Sorgu(StokHareket.class);
                        satilanMiktarSorgu.kriterEkle(KriterFactory.esit("tabanTavanKaldirmaTalep", talep));
                        satilanMiktarSorgu.kriterEkle(KriterFactory.bos("stokFis.iptalEdenFis"));
                        satilanMiktarSorgu.kriterEkle(KriterFactory.esit("stokFis.kurum", kurumStok.getKurum()));
                        List<StokHareket> satilanMiktarHareketler = sorgula(satilanMiktarSorgu);
                        for (StokHareket hareket : satilanMiktarHareketler) {
                            satilanMiktar = satilanMiktar.add(hareket.getMiktar());
                        }

                        flush();
                        satilanMiktarHareketler = null;

                        selectItems.add(new SelectItem(talep, "Talep Tipi: " + commonServis.bundleAl(talep.getTip().getEtiket()) + " ## Fiyat: " + talep.getTalepFiyat()
                                + "  ## Toplam Miktar: " + talep.getMiktar() + " ## Kalan Bakiye: " + talep.getMiktar().subtract(satilanMiktar) + " ## Geçerlilik Tarihi: " + talep.getOnayTarihi().gunEkle(3)));
                    }
                }

            }

        }
        return selectItems;
    }

    public void tabanTavanTalepMiktarKontrol(KoopSatisFis fis) throws BusinessRuleException {

        for (StokHareket stokHareket : fis.getHareketler()) {
            KoopSatisFisHareket satisFisHareket = (KoopSatisFisHareket) stokHareket;
            if (null != satisFisHareket.getTabanTavanKaldirmaTalep()) {

                BigDecimal satilanMiktar = BigDecimal.ZERO;

                Sorgu satilanMiktarSorgu = new Sorgu(StokHareket.class);
                satilanMiktarSorgu.kriterEkle(KriterFactory.esit("tabanTavanKaldirmaTalep", satisFisHareket.getTabanTavanKaldirmaTalep()));
                satilanMiktarSorgu.kriterEkle(KriterFactory.bos("stokFis.iptalEdenFis"));
                satilanMiktarSorgu.kriterEkle(KriterFactory.esit("stokFis.kurum", fis.getKurum()));
                List<StokHareket> satilanMiktarHareketler = sorgula(satilanMiktarSorgu);
                for (StokHareket hareket2 : satilanMiktarHareketler) {
                    satilanMiktar = satilanMiktar.add(hareket2.getMiktar());
                }

                if (satilanMiktar.compareTo(satisFisHareket.getTabanTavanKaldirmaTalep().getMiktar()) > 0)
                    throw new BusinessRuleException("Bölge Birliginin Onayladığı Miktarı Aşan Satış Yapamazsınız.");
            }
        }

    }

    public Senet veresiyeIslemListesindenSenetIslemListesiVeSenetOlustur(List<VeresiyeIslem> veresiyeIslemleri, Tarih normalVadeBitisTarihi) throws BusinessRuleException {

        List<SenetIslem> senetIslemler = new ArrayList<SenetIslem>();
        Tarih vadeBitisTarihiSaatli = new Tarih(normalVadeBitisTarihi, 23, 59, 59);

        VeresiyeIslem stokSatisVeresiyeIslem = null;
        BigDecimal vadeSonunaKadarkiAnapara = BigDecimal.ZERO;
        for (VeresiyeIslem vi : veresiyeIslemleri) {
            if (vi.getIptalTarihi() == null) {
                if (vi.getVeresiyeIslemTipi().equals(VeresiyeIslemTipi.STOK_FATURA_DUZENLEME)) {
                    stokSatisVeresiyeIslem = vi;
                } else {
                    SenetIslemTipi senetIslemTipi = VeresiyeIslemTipi.veresiyeIslemTipiniSenetIslemTipineCevir(vi.getVeresiyeIslemTipi());
                    SenetIslem senetIslem = null;
                    if (vi.getVeresiyeIslemTipi().equals(VeresiyeIslemTipi.TAHSILAT))
                        senetIslem = new Tahsilat(senetIslemTipi, vi.getIslemTarihi(), vi.getValorTarihi(), vi.getBorc(), vi.getAlacak(), vi.getValorDegisti(), vi.getAciklama(), vi.getTahsilatTipi(), null);
                    else
                        senetIslem = new SenetIslem(senetIslemTipi, vi.getIslemTarihi(), vi.getValorTarihi(), vi.getBorc(), vi.getAlacak(), vi.getValorDegisti(), vi.getAciklama(), null);
                    senetIslemler.add(senetIslem);
                }

                if (vi.getIslemTarihi().beforeOrEqual(vadeBitisTarihiSaatli) && vi.getValorTarihi().beforeOrEqual(normalVadeBitisTarihi.gunEkle(1))) {
                    vadeSonunaKadarkiAnapara = vadeSonunaKadarkiAnapara.add(vi.getBorc().subtract(vi.getAlacak()));
                }
            }
        }

        senetIslemler.add(new SenetIslem(SenetIslemTipi.SENET_DUZENLEME, stokSatisVeresiyeIslem.getIslemTarihi(), stokSatisVeresiyeIslem.getValorTarihi(),
                vadeSonunaKadarkiAnapara, BigDecimal.ZERO, stokSatisVeresiyeIslem.getValorDegisti(), stokSatisVeresiyeIslem.getAciklama(), null));

        Senet tempSenet = ortakKrediModulServis.veresiyeTemerrutHesaplamaIcinSenetNesnesiYapilandir(senetIslemler, normalVadeBitisTarihi);

        return tempSenet;
    }
    public StokGruplari getStokGruplari(StokGrup grup) {
        StokGruplari stokGruplari = new StokGruplari();
        stokGruplari.setAlisKdvOrani(dao.getStokGruplariAlisKdvOran(grup));
        stokGruplari.setSatisKdvOrani(dao.getStokGruplariSatisKdvOran(grup));
        stokGruplari.setStokMuhasebeHesabi(dao.getStokGruplariStokMuhasebeHesabi(grup));
        stokGruplari.setKonsinyeMuhasebeHesabi(dao.getStokGruplariKonsinyeMuhasebeHesabi(grup));

        return  stokGruplari;
    }



    public List<SabitFiyatStokIl> maxTarihliSabitFiyatStokIller( SabitFiyatStok sabitFiyatStok){
        return dao.maxTarihliSabitFiyatStokIller( sabitFiyatStok );
    }

    public void protokolGecersizYap(Ortak ortak, String aciklama)throws BusinessRuleException{

        Sorgu sorgu = new Sorgu(Protokol.class);
        sorgu.kriterEkle(KriterFactory.esit("ortak", ortak));
        sorgu.kriterEkle(KriterFactory.esit("durum", ProtokolDurum.ONAYLI));
        List<Protokol> protokolList = sorgula( sorgu );

        if( !protokolList.isEmpty() ){
            protokolList.get(0).setDurum(ProtokolDurum.GECERSIZ);
            protokolList.get(0).setGecersizlikTarihi(getSimdikiTarih());
            protokolList.get(0).setGecersizlikAciklama( aciklama );

            kaydet( protokolList );
        }


    }

    @Override
    public void veresiyeTahsilatKaydetMahsuplu(VeresiyeIslem veresiyeIslem, BigDecimal vadesiGecenFarkTutari, Kullanici kullanici, boolean temerrutDonemiMi) throws BusinessRuleException {

        kaydet(veresiyeIslem);


        Hesap faizHesap = muhasebeModulServis.altHesapBulYoksaAc("642900002" + veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo() , veresiyeIslem.getStokFis().getMuhatap().getOrtak().getNo() + " " + veresiyeIslem.getStokFis().getMuhatap().getOrtak().getIsim() + " İADE HESABI",veresiyeIslem.getStokFis().getKurum());

        Hesap firma125Hesap = muhasebeModulServis.altHesapBulYoksaAc("125800011" + veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo() , veresiyeIslem.getStokFis().getMuhatap().getOrtak().getNo() + " " + veresiyeIslem.getStokFis().getMuhatap().getOrtak().getIsim() + " İADE HESABI",veresiyeIslem.getStokFis().getKurum());

        Hesap _926Hesap = muhasebeModulServis.altHesapBulYoksaAc("926000001"+veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo(),  veresiyeIslem.getStokFis().getMuhatap().getIsim(), veresiyeIslem.getStokFis().getKurum());
        Hesap _927Hesap = muhasebeModulServis.altHesapBulYoksaAc("927000001"+veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo(),  veresiyeIslem.getStokFis().getMuhatap().getIsim(), veresiyeIslem.getStokFis().getKurum());

        MahsupFis tahsilatMahsubuFis = new MahsupFis(getSimdikiTarih(), veresiyeIslem.getKurum(), FisKaynak.STOK, "STOK VERESIYE SATIŞ TAHSİLAT MAHSUBU");
        tahsilatMahsubuFis.setTip(FisTip.MAHSUP);
        tahsilatMahsubuFis.setKullanici(kullanici);

        if (veresiyeIslem.getTahsilatTipi().equals(TahsilatTipi.MAHSUBEN)) {
            MahsupFis mahsupFis = new MahsupFis(getSimdikiTarih(), veresiyeIslem.getKurum(), FisKaynak.STOK, "STOK VERESIYE SATIŞ TAHSİLAT");
            mahsupFis.setTip(FisTip.MAHSUP);
            mahsupFis.setKullanici(kullanici);

            if( temerrutDonemiMi ){

                if( veresiyeIslem.getAlacak().compareTo( vadesiGecenFarkTutari )> 0){
                    mahsupFis.hareketEkle(new FisHareket(mahsupFis, firma125Hesap, BigDecimal.ZERO, veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT"));
                    mahsupFis.hareketEkle(new FisHareket(mahsupFis, faizHesap, BigDecimal.ZERO,   vadesiGecenFarkTutari , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT"));

                    tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _926Hesap, BigDecimal.ZERO, veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
                    tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _927Hesap,veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ) ,BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));

                }else{
                    mahsupFis.hareketEkle(new FisHareket(mahsupFis, veresiyeIslem.getStokFis().getFirmaMuhatapHesap(), BigDecimal.ZERO, veresiyeIslem.getAlacak(), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT"));

                    tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _926Hesap, BigDecimal.ZERO, veresiyeIslem.getAlacak(), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
                    tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _927Hesap,veresiyeIslem.getAlacak(),BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));

                }



            }else{
                mahsupFis.hareketEkle(new FisHareket(mahsupFis, veresiyeIslem.getStokFis().getFirmaMuhatapHesap(), BigDecimal.ZERO, veresiyeIslem.getAlacak(), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT"));

                tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _926Hesap, BigDecimal.ZERO, veresiyeIslem.getAlacak(), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
                tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _927Hesap,veresiyeIslem.getAlacak(),BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
            }
            mahsupFis.hareketEkle(new FisHareket(mahsupFis, veresiyeIslem.getMahsupHesap(), veresiyeIslem.getAlacak(), BigDecimal.ZERO, "ORTAK ICI VERESIYE SATIŞ TAHSİLAT"));


            veresiyeIslem.setMahsupFisi(muhasebeModulServis.kaliciMahsupFisiAc(mahsupFis));



        }

        if (veresiyeIslem.getTahsilatTipi().equals(TahsilatTipi.TAHSIL)) {
            KasaFis tahsilFis = new KasaFis(getSimdikiTarih(), veresiyeIslem.getKurum(), FisKaynak.STOK, "STOK VERESIYE SATIŞ TAHSİLAT", FisParaBirimi.TL);
            tahsilFis.setTip(FisTip.TAHSIL);





            if( temerrutDonemiMi ){

                if( veresiyeIslem.getAlacak().compareTo( vadesiGecenFarkTutari )> 0){
                    tahsilFis.hareketEkle(new FisHareket(tahsilFis, firma125Hesap, BigDecimal.ZERO, veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT"));
                    tahsilFis.hareketEkle(new FisHareket(tahsilFis, faizHesap, BigDecimal.ZERO,   vadesiGecenFarkTutari , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT"));

                    tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _926Hesap, BigDecimal.ZERO, veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
                    tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _927Hesap,veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ),BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));

                }else{
                    tahsilFis.hareketEkle(new FisHareket(tahsilFis, faizHesap, BigDecimal.ZERO, veresiyeIslem.getAlacak(), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT"));

                    tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _926Hesap, BigDecimal.ZERO, veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
                    tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _927Hesap,veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ),BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));


                }



            }else{
                tahsilFis.hareketEkle(new FisHareket(tahsilFis, veresiyeIslem.getStokFis().getFirmaMuhatapHesap(), BigDecimal.ZERO, veresiyeIslem.getAlacak(), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT"));

                tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _926Hesap, BigDecimal.ZERO, veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ), "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
                tahsilatMahsubuFis.hareketEkle(new FisHareket(tahsilatMahsubuFis, _927Hesap,veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ),BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));

            }


            veresiyeIslem.setTahsilFis(muhasebeModulServis.kasaFisiKes(tahsilFis, kullanici));
        }

        veresiyeIslem.setTahsilatMahsupFisi(muhasebeModulServis.kaliciMahsupFisiAc(tahsilatMahsubuFis));

    }

    @Override
    public void veresiyeTahsilatIptal(VeresiyeIslem veresiyeIslem,BigDecimal vadesiGecenFarkTutari , Kullanici kullanici) throws BusinessRuleException {

        Boolean temerrutDonemiMi = false;
        Hesap faizHesap = muhasebeModulServis.altHesapBulYoksaAc("642900002" + veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo() , veresiyeIslem.getStokFis().getMuhatap().getOrtak().getNo() + " " + veresiyeIslem.getStokFis().getMuhatap().getOrtak().getIsim() + " İADE HESABI",veresiyeIslem.getStokFis().getKurum());


        Hesap _926Hesap = muhasebeModulServis.altHesapBulYoksaAc("926000001"+veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo(),  veresiyeIslem.getStokFis().getMuhatap().getIsim(), veresiyeIslem.getStokFis().getKurum());
        Hesap _927Hesap = muhasebeModulServis.altHesapBulYoksaAc("927000001"+veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo(),  veresiyeIslem.getStokFis().getMuhatap().getIsim(), veresiyeIslem.getStokFis().getKurum());


        Fis kasaFis = null;

        if( veresiyeIslem.getTahsilatTipi().equals( TahsilatTipi.TAHSIL)){

            kasaFis = veresiyeIslem.getTahsilFis();

        }else if( veresiyeIslem.getTahsilatTipi().equals( TahsilatTipi.MAHSUBEN)){
            kasaFis = veresiyeIslem.getMahsupFisi();
        }


        for( FisHareket kasaHareket : kasaFis.getFisHareketleri() ){
            if( kasaHareket.getHesap().equals( faizHesap ))
                temerrutDonemiMi = true;
        }

        if (veresiyeIslem.getTahsilatTipi().equals(TahsilatTipi.TAHSIL)) {


            if( temerrutDonemiMi ){


                Hesap mahsupHesap = muhasebeModulServis.altHesapBulYoksaAc("331900001" + veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo() , veresiyeIslem.getStokFis().getMuhatap().getOrtak().getNo() + " " + veresiyeIslem.getStokFis().getMuhatap().getOrtak().getIsim() + " İADE HESABI",veresiyeIslem.getStokFis().getKurum());

                Hesap firma125Hesap = muhasebeModulServis.altHesapBulYoksaAc("125800011" + veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo() , veresiyeIslem.getStokFis().getMuhatap().getOrtak().getNo() + " " + veresiyeIslem.getStokFis().getMuhatap().getOrtak().getIsim() + " İADE HESABI",veresiyeIslem.getStokFis().getKurum());


                MahsupFis mahsupFis = new MahsupFis(getSimdikiTarih(), veresiyeIslem.getStokFis().getKurum(), FisKaynak.STOK, veresiyeIslem.getTahsilFis().getAciklama() + " İPTAL");
                muhasebeModulServis.geciciMahsupFisiAc(mahsupFis, kullanici);
                mahsupFis.setTip(FisTip.MAHSUP);


                for (FisHareket kasaHareket : kasaFis.getFisHareketleri()) {
                    if (kasaHareket.getHesap().isKasaHesabi()) {
                        FisHareket tediyeHareket = new FisHareket(mahsupFis, mahsupHesap,
                                BigDecimal.ZERO, kasaHareket.getBorc() , kasaHareket.getAciklama() + " İPTAL", kasaHareket.getBelgeTipi(), kasaHareket.getBelgeNo(), kasaHareket.getBelgeTarihi(), null);


                        if (kasaHareket.getValorTarihi() != null) {
                            tediyeHareket.setValorTarihi(kasaHareket.getValorTarihi());
                        }


                        mahsupFis.hareketEkle(tediyeHareket);

                        mahsupFis.hareketEkle(new FisHareket(mahsupFis, _926Hesap, kasaHareket.getBorc() , BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
                        mahsupFis.hareketEkle(new FisHareket(mahsupFis, _927Hesap, BigDecimal.ZERO , kasaHareket.getBorc() , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));






                    }else if( kasaHareket.getHesap().equals( faizHesap)){
                        FisHareket tediyeHareket2 = new FisHareket(mahsupFis, faizHesap, kasaHareket.getAlacak() , BigDecimal.ZERO, "ORTAK ICI VERESIYE SATIŞ TAHSİLAT İPTAL");
                        mahsupFis.hareketEkle(tediyeHareket2);
                    }
                    else{
                        FisHareket tediyeHareket2 = new FisHareket(mahsupFis, firma125Hesap, kasaHareket.getAlacak() , BigDecimal.ZERO, "ORTAK ICI VERESIYE SATIŞ TAHSİLAT İPTAL");
                        mahsupFis.hareketEkle(tediyeHareket2);

                        mahsupFis.hareketEkle(new FisHareket(mahsupFis, _926Hesap, veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ) , BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
                        mahsupFis.hareketEkle(new FisHareket(mahsupFis, _927Hesap, BigDecimal.ZERO , veresiyeIslem.getAlacak().subtract( vadesiGecenFarkTutari ) , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));

                    }
                }



                muhasebeModulServis.kaliciMahsupFisiAc(mahsupFis);

                veresiyeIslem.setIptalFis( mahsupFis );
                veresiyeIptalBagliHareketlerDuzenle( veresiyeIslem, kullanici  );




            }else{
                Hesap mahsupHesap = muhasebeModulServis.altHesapBulYoksaAc("331900001" + veresiyeIslem.getStokFis().getMuhatap().getMuhasebe5HesapNo() , veresiyeIslem.getStokFis().getMuhatap().getOrtak().getNo() + " " + veresiyeIslem.getStokFis().getMuhatap().getOrtak().getIsim() + " İADE HESABI",veresiyeIslem.getStokFis().getKurum());

                MahsupFis mahsupFis = new MahsupFis(getSimdikiTarih(), veresiyeIslem.getStokFis().getKurum(), FisKaynak.STOK, veresiyeIslem.getTahsilFis().getAciklama() + " İPTAL");
                muhasebeModulServis.geciciMahsupFisiAc(mahsupFis, kullanici);
                mahsupFis.setTip(FisTip.MAHSUP);
                kasaFis = veresiyeIslem.getTahsilFis();
                kasaFis = yukle(KasaFis.class, kasaFis.getId());

                for (FisHareket kasaHareket : kasaFis.getFisHareketleri()) {
                    if (kasaHareket.getHesap().isKasaHesabi()) {
                        FisHareket tediyeHareket = new FisHareket(mahsupFis, mahsupHesap,
                                BigDecimal.ZERO, kasaHareket.getBorc() , kasaHareket.getAciklama() + " İPTAL", kasaHareket.getBelgeTipi(), kasaHareket.getBelgeNo(), kasaHareket.getBelgeTarihi(), null);


                        if (kasaHareket.getValorTarihi() != null) {
                            tediyeHareket.setValorTarihi(kasaHareket.getValorTarihi());
                        }


                        mahsupFis.hareketEkle(tediyeHareket);


                        FisHareket tediyeHareket2 = new FisHareket(mahsupFis, veresiyeIslem.getStokFis().getFirmaMuhatapHesap(),
                                veresiyeIslem.getAlacak(), BigDecimal.ZERO, "ORTAK ICI VERESIYE SATIŞ TAHSİLAT İPTAL");
                        mahsupFis.hareketEkle(tediyeHareket2);


                        //todo burada attığımız bakiye doğru mu kontrol edelim
                        mahsupFis.hareketEkle(new FisHareket(mahsupFis, _926Hesap, veresiyeIslem.getAlacak() , BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
                        mahsupFis.hareketEkle(new FisHareket(mahsupFis, _927Hesap, BigDecimal.ZERO , veresiyeIslem.getAlacak() , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));





                    }

                    if( kasaHareket.getHesap().equals( faizHesap)){
                        FisHareket tediyeHareket2 = new FisHareket(mahsupFis, faizHesap, kasaHareket.getAlacak() , BigDecimal.ZERO, "ORTAK ICI VERESIYE SATIŞ TAHSİLAT İPTAL");
                        mahsupFis.hareketEkle(tediyeHareket2);
                    }
                }



                muhasebeModulServis.kaliciMahsupFisiAc(mahsupFis);

                veresiyeIslem.setIptalFis( mahsupFis );
                veresiyeIptalBagliHareketlerDuzenle( veresiyeIslem, kullanici  );
            }


        } else {
            MahsupFis mahsupFis = muhasebeModulServis.tersMahsupOlustur(veresiyeIslem.getMahsupFisi(), getSimdikiTarih(), kullanici);

            mahsupFis.hareketEkle(new FisHareket(mahsupFis, _926Hesap, veresiyeIslem.getMahsupFisi().getSumBorc() , BigDecimal.ZERO , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));
            mahsupFis.hareketEkle(new FisHareket(mahsupFis, _927Hesap, BigDecimal.ZERO , veresiyeIslem.getMahsupFisi().getSumBorc() , "ORTAK ICI VERESIYE SATIŞ TAHSİLAT MAHSUBU"));

            muhasebeModulServis.kaliciMahsupFisiAc(mahsupFis);
            veresiyeIslem.setIptalFis( mahsupFis );
            veresiyeIptalBagliHareketlerDuzenle( veresiyeIslem, kullanici  );



        }
    }

    public void veresiyeIptalBagliHareketlerDuzenle(VeresiyeIslem veresiyeIslem, Kullanici kullanici) throws BusinessRuleException {
        veresiyeIslem.setIptalDurum(SenetIslemIptalDurum.IPTAL_EDILDI);
        veresiyeIslem.setIptalEdenKullanici(kullanici);
        veresiyeIslem.setIptalFis(veresiyeIslem.getIptalFis());
        veresiyeIslem.setIptalTarihi(getSimdikiTarih());
        kaydet(veresiyeIslem);

        Sorgu tahsilataBagliHareketlerSorgu = new Sorgu( VeresiyeIslem.class);
        tahsilataBagliHareketlerSorgu.kriterEkle(KriterFactory.esit("kurum", veresiyeIslem.getKurum()));
        tahsilataBagliHareketlerSorgu.kriterEkle(KriterFactory.esit("islemTarihi", veresiyeIslem.getIslemTarihi()));
//        tahsilataBagliHareketlerSorgu.kriterEkle(KriterFactory.esit("stokFis", veresiyeIslem.getStokFis()));
        tahsilataBagliHareketlerSorgu.kriterEkle(KriterFactory.esitDegil("id", veresiyeIslem.getId()));

        Sorgu odendiSorgu = new Sorgu( VeresiyeIslem.class);
        odendiSorgu.kriterEkle(KriterFactory.esit("kurum", veresiyeIslem.getKurum()));
        odendiSorgu.kriterEkle(KriterFactory.esit("veresiyeIslemTipi", VeresiyeIslemTipi.ODENDI));
        odendiSorgu.kriterEkle(KriterFactory.esit("stokFis", veresiyeIslem.getStokFis()));



        List<VeresiyeIslem> odendiHareketler = sorgula( odendiSorgu );
        List<VeresiyeIslem> tahsilataBagliHareketler = sorgula( tahsilataBagliHareketlerSorgu );

        if( !tahsilataBagliHareketler.isEmpty() ){
            VeresiyeIslem faizTahakkukIslem = tahsilataBagliHareketler.get( 0 );

            faizTahakkukIslem.setIptalDurum(SenetIslemIptalDurum.IPTAL_EDILDI);
            faizTahakkukIslem.setIptalEdenKullanici(kullanici);
            faizTahakkukIslem.setIptalFis(veresiyeIslem.getMahsupFisi() );
            faizTahakkukIslem.setIptalTarihi( veresiyeIslem.getIptalTarihi() );
        }

        if( !odendiHareketler.isEmpty() ){
            sil( odendiHareketler );
        }
    }

    public BigDecimal temerrutTutar(List<VeresiyeIslem>  veresiyeIslemList, Tarih normalVadeBitisTarihi, Tarih islemTarihi, Tarih valorTarihi) throws BusinessRuleException {
        return ortakKrediModulServis.veresiyeTemerrutFaizHesapla(veresiyeIslemList,  normalVadeBitisTarihi, islemTarihi, valorTarihi);
    }

    public List<VeresiyeIslem> veresiyeTahsilatiIcinSenetIslemler(List<VeresiyeIslem> veresiyeIslemList,
                                                                  Tarih normalVadeBitisTarihi, Tarih tahsilatIslemTarihi, Tarih tahsilatValorTarihi,
                                                                  BigDecimal tahsilatTutari, TahsilatTipi tahsilatTipi, Hesap mahsupHesap) throws BusinessRuleException{
        Tarih sonrakiIlkgun = tahsilatIslemTarihi.getSaatsizTarih().gunEkle(1);
        if (tahsilatTipi.equals(TahsilatTipi.TAHSIL) && !tahsilatValorTarihi.esit(sonrakiIlkgun)) {
            throw new BusinessRuleException(OrtakKrediHatalar.TAHSIL_FISI_ILE_YAPILAN_TAHSILATLARDA_VALOR_SISTEM_TARIHINDEN_BIR_SONRAKI_GUN_DISINDA_OLAMAZ);
        }
        if (tahsilatTipi.equals(TahsilatTipi.MAHSUBEN) && tahsilatValorTarihi.after(sonrakiIlkgun)) {
            throw new BusinessRuleException(OrtakKrediHatalar.ILERIYE_YONELIK_TAHSILAT_YAPILAMAZ);
        }

        if (tahsilatTipi.equals(TahsilatTipi.MAHSUBEN) && mahsupHesap == null)
            throw new BusinessRuleException(OrtakKrediHatalar.MAHSUBEN_TAHSILATLARDA_MAHSUP_MUAVIN_HESAP_GIRILMELIDIR);

        if( tahsilatTipi.equals(TahsilatTipi.MAHSUBEN)){

            String hesapNo = mahsupHesap.getHesapNo();
            boolean hesabinBakiyesiKontrolEdilecek = (!(hesapNo.startsWith("135200001") || hesapNo.startsWith("127060001") || hesapNo.startsWith("127060002") || hesapNo.startsWith("127080001")));


            if (tahsilatTipi.equals(TahsilatTipi.MAHSUBEN) && hesabinBakiyesiKontrolEdilecek) {
                //Aşağıdaki if için eğer vadedeki faiz tahsilatı yapılıyorsa ve kooperatifin henüz devir fişi yapılmamışsa kontrole girme (Ocak ayında çıkan vadedeki faiz tahsilatı hatası)
                if (muhasebeModulServis.yilSonuKapanisFisTarihi(mahsupHesap.getKurum(), tahsilatValorTarihi.getYil() - 1) == null) {
                    HesapBakiye bakiye = muhasebeModulServis.getAltHesapBakiyesi(mahsupHesap.getKurum(), mahsupHesap, tahsilatIslemTarihi.getYil());

                    if (bakiye == null || bakiye.getAlacakBakiye().doubleValue() < tahsilatTutari.doubleValue()) {
                        String parametre = bakiye != null ? bakiye.getAlacakBakiye().toString() : "";
                        throw new BusinessRuleException(OrtakKrediHatalar.MAHSUP_HESAPTA_YETERLI_ALACAK_BAKIYESI_YOK, parametre);
                    }
                }
            }
        }



        return ortakKrediModulServis.veresiyeTahsilatiIcinSenetIslemler( veresiyeIslemList, normalVadeBitisTarihi, tahsilatIslemTarihi, tahsilatValorTarihi, tahsilatTutari, tahsilatTipi);
    }

    public BigDecimal vadeliVeresiyeKalanBakiye(KoopSatisFis satisFis) throws BusinessRuleException {
        BigDecimal versiyeSatisLimiti = BigDecimal.ZERO;
        if( satisFis.getMuhatap().getTip().equals(GercekTuzel.TUZEL)){
            TuzelFirmaMusteri tfm = (TuzelFirmaMusteri) satisFis.getMuhatap();

            if(tfm.getKamuMu())
                versiyeSatisLimiti = new BigDecimal(12500);
            else
                versiyeSatisLimiti = satisFis.getProtokol().getVeresiyeSatisLimiti();
        }else
            versiyeSatisLimiti = satisFis.getProtokol().getVeresiyeSatisLimiti();





        BigDecimal veresiyeTutar = satisFis.getSumMOTTutar().subtract(satisFis.getMahsubenTahsilatTutar() == null ? BigDecimal.ZERO : satisFis.getMahsubenTahsilatTutar());
        if (EkoopUtils.isBuyuk(veresiyeTutar, versiyeSatisLimiti)) {
            throw new BusinessRuleException(StokHataKodu.VERESIYE_SATISLARDA_ORTAGIN_KREDI_LIMITININ_YUZDE20_GECEMEZ);
        }

        //onceki veresiye satislari sisteme dahil edip genel toplam aliyoruz.
        BigDecimal veresiye121HesapBakiye = BigDecimal.ZERO;
        BigDecimal veresiye101HesapBakiye = BigDecimal.ZERO;
        BigDecimal veresiye121HesapBakiye2 = BigDecimal.ZERO;
        BigDecimal veresiye125HesapBakiye = BigDecimal.ZERO;
        String altKirilim = "";
        if ((satisFis).getMuhatapTip().equals(MuhatapTip.ORTAK_ICI)) {
            altKirilim = satisFis.getMuhatap().getMuhasebe5HesapNo();
        }

        if ((satisFis).getMuhatapTip().equals(MuhatapTip.ORTAK_DISI)) {
            altKirilim = satisFis.getMuhatap().getMuhasebe5HesapNo();
        }
        List<Hesap> ortakVeresiye121Hesap;
        Hesap ortakVeresiye101Hesap;
        Hesap ortakVeresiye121Hesap2;


        try{
            Sorgu hesapSorgu = new Sorgu(Hesap.class);
            hesapSorgu.kriterEkle(KriterFactory.esit("hesapNo", "121200001" + altKirilim ));
            ortakVeresiye121Hesap = muhasebeModulServis.getKurumaAitAcikAltHesaplar( satisFis.getKurum(), hesapSorgu);
            if( !ortakVeresiye121Hesap.isEmpty() )
            veresiye121HesapBakiye = muhasebeModulServis.getAltHesapBakiyesiTariheKadar(satisFis.getKurum(), ortakVeresiye121Hesap.get( 0 ), satisFis.getFisTarihi()).getAbsoluteBakiye();

        }catch (BusinessRuleException e){
            veresiye121HesapBakiye = BigDecimal.ZERO;
        }

//        try{
//            ortakVeresiye101Hesap = muhasebeModulServis.getKurumaAitAcikHesapByNo("101000001" + altKirilim, satisFis.getKurum());
//            veresiye101HesapBakiye = muhasebeModulServis.getAltHesapBakiyesiTariheKadar(satisFis.getKurum(), ortakVeresiye101Hesap, satisFis.getFisTarihi()).getAbsoluteBakiye();
//
//        }catch (BusinessRuleException e){
//            veresiye101HesapBakiye = BigDecimal.ZERO;
//        }

        ortakVeresiye121Hesap2 = muhasebeModulServis.getKurumaAitAcikHesapByNo("121200002" + altKirilim, satisFis.getKurum());
        veresiye121HesapBakiye2 = muhasebeModulServis.getAltHesapBakiyesiTariheKadar(satisFis.getKurum(), ortakVeresiye121Hesap2, satisFis.getFisTarihi()).getAbsoluteBakiye();

        List<Hesap> ortakVeresiye125Hesap;
        try{
            Sorgu hesapSorgu = new Sorgu(Hesap.class);
            hesapSorgu.kriterEkle(KriterFactory.esit("hesapNo", "125800011" + altKirilim ));
            ortakVeresiye125Hesap = muhasebeModulServis.getKurumaAitAcikAltHesaplar( satisFis.getKurum(), hesapSorgu);
            if( !ortakVeresiye125Hesap.isEmpty() )
                veresiye125HesapBakiye =  muhasebeModulServis.getAltHesapBakiyesiTariheKadar(satisFis.getKurum(), ortakVeresiye125Hesap.get(0), satisFis.getFisTarihi()).getAbsoluteBakiye();

        }catch (BusinessRuleException e){
            veresiye125HesapBakiye = BigDecimal.ZERO;
        }



        flush();

        veresiyeTutar = veresiyeTutar.add(veresiye121HesapBakiye);
        veresiyeTutar = veresiyeTutar.add(veresiye121HesapBakiye2);
        veresiyeTutar = veresiyeTutar.add(veresiye125HesapBakiye);

        if (EkoopUtils.isBuyuk(veresiyeTutar, versiyeSatisLimiti)) {
            throw new BusinessRuleException("Veresiye Satış Limitini Aşamazsınız.");
        }
            return versiyeSatisLimiti.subtract( veresiyeTutar );
    }



    public StokGrup stokunUcuncuSeviyeGrubu(Stok stok) {

        int i = 0;
        StokGrup stokGrup = stok.getGrup();
        while(i<20) {
            if(stokGrup.getKod().length()==9)
                break;
            else
                stokGrup = stokGrup.getUstGrup();
            i++;
        }

        return stokGrup;
    }


    public void ortagin121EskiBorcuVarMi(Ortak ortak) throws BusinessRuleException {


        List <Hesap> ortakVeresiye121Hesap;
        BigDecimal veresiye121HesapBakiye = BigDecimal.ZERO;
        Sorgu hesapSorgu = new Sorgu( Hesap.class );
        hesapSorgu.kriterEkle(KriterFactory.esit("hesapNo","121200001" + ortak.get5No()));
        hesapSorgu.kriterEkle(KriterFactory.esit("kurum", ortak.getKurum()));
        ortakVeresiye121Hesap = sorgula( hesapSorgu );

        if( !ortakVeresiye121Hesap.isEmpty()){
            veresiye121HesapBakiye = muhasebeModulServis.getAltHesapBakiyesiTariheKadar(ortak.getKurum(), ortakVeresiye121Hesap.get( 0 ), getSimdikiTarih()).getAbsoluteBakiye();

        }
        if( veresiye121HesapBakiye.compareTo( BigDecimal.ZERO) > 0)
            throw new BusinessRuleException("Ortağın 121.20.0001 Hesapta "+veresiye121HesapBakiye+" TL Borcu Bulunmaktadır. Borcun Muhasebe Modülünden Tahsilinden Sonra Satış Yapabilirsiniz.");


        String hesapAciklama = null;

        if( ortak.getTip().equals( GercekTuzel.GERCEK)){

            GercekKisiOrtak ort = (GercekKisiOrtak)yukle(GercekKisiOrtak.class, ortak.getId());
            hesapAciklama = ort.getAd()+" "+ort.getSoyad();
        }else if( ortak.getTip().equals( GercekTuzel.TUZEL)){
            TuzelKisiOrtak ort = yukle(TuzelKisiOrtak.class, ortak.getId());
            hesapAciklama = ort.getUnvan();
        }



        List <Hesap> ortakVeresiye125Hesap;
        BigDecimal veresiye125HesapBakiye = BigDecimal.ZERO;
        Sorgu hesap125Sorgu = new Sorgu( Hesap.class );
        hesap125Sorgu.kriterEkle(KriterFactory.esit("hesapNo","125800011" + ortak.get5No()));
        hesap125Sorgu.kriterEkle(KriterFactory.esit("kurum", ortak.getKurum()));
        ortakVeresiye125Hesap = sorgula( hesap125Sorgu );

        if( !ortakVeresiye125Hesap.isEmpty()){
            veresiye125HesapBakiye = muhasebeModulServis.getAltHesapBakiyesiTariheKadar(ortak.getKurum(), ortakVeresiye125Hesap.get( 0 ), getSimdikiTarih()).getAbsoluteBakiye();

        }
        if( veresiye125HesapBakiye.compareTo( BigDecimal.ZERO) > 0)
            throw new BusinessRuleException("Ortağın Vadesi Geçen "+veresiye125HesapBakiye+" TL Tutarında Veresiye Borcu Bulunmaktadır. Protokol Düzenlenemez.");


        //Hata yoksa yeni 120 hesabı da açalım
        Hesap yeni121Hesap = muhasebeModulServis.altHesapBulYoksaAc( "121200002" + ortak.get5No(), hesapAciklama, ortak.getKurum() );

        kaydet( yeni121Hesap );

    }

    public  void ortaginIdariTakipteBorcuVarMi(Ortak ortak) throws BusinessRuleException{


            if (ortakKrediModulServis.isOrtaginVadesiGecenBorcuVar( ortak )) {
                throw new BusinessRuleException("Vadesi Geçen Kredi Borcu Olan Ortağa Veresiye Satış Protokolü Düzenleyemezsiniz.");
            }


    }

    public void protokolKaydet(Protokol protokol, FormModu formModu) throws BusinessRuleException {

        ortaginGecerliProtokoluVarMi( protokol );

        if( formModu.equals(FormModu.EKLE))
            protokol.setNo(kurumMaxProtokolNo( protokol.getKurum() )+1);

        protokol.setTarih(  getSimdikiTarih());
        kaydet( protokol );


    }

    public void ortaginGecerliProtokoluVarMi( Protokol protokol) throws BusinessRuleException {


        Sorgu protokolSorgu = new Sorgu(Protokol.class);
        protokolSorgu.kriterEkle(KriterFactory.esit("ortak", protokol.getOrtak()));
        protokolSorgu.kriterEkle(KriterFactory.or(  KriterFactory.esit("durum", ProtokolDurum.ONAYLI)  , KriterFactory.esit("durum", ProtokolDurum.GECICI)));
        List<Protokol> onayliProtokolList = sorgula( protokolSorgu );
        if( !onayliProtokolList.isEmpty() )
            throw new BusinessRuleException("Ortağın Mevcut Onaylı veya Geçici Protokolü Bulunmaktadır.");
    }

    public Integer kurumMaxProtokolNo( Kurum kurum){

        Sorgu maxProtokolSorgu = new Sorgu(Protokol.class);
        maxProtokolSorgu.kriterEkle(KriterFactory.esit("kurum", kurum));
        maxProtokolSorgu.setSiralamaKriteri(new SiralamaKriteri("no",false));
        List<Protokol> protokolList = sorgula( maxProtokolSorgu );
        if( !protokolList.isEmpty())
            return  protokolList.get(0).getNo();
        else
            return 0;

    }

    public void takipKaldir( VeresiyeIslem islem, Kullanici kullanici) throws BusinessRuleException {


        MahsupFis mahsupFis = null;
        Hesap firma125Hesap = ModulServisFactroyBean.getIMuhasebeServis().altHesapBulYoksaAc("125800011" + islem.getStokFis().getMuhatap().getMuhasebe5HesapNo() , islem.getStokFis().getMuhatap().getMuhasebe5HesapNo() + " " + islem.getStokFis().getMuhatap().getIsim() + " TAKIP HESABI", islem.getKurum());
        Hesap firma121Hesap = ModulServisFactroyBean.getIMuhasebeServis().altHesapBulYoksaAc("121200002" + islem.getStokFis().getMuhatap().getMuhasebe5HesapNo() , islem.getStokFis().getMuhatap().getMuhasebe5HesapNo() + " " + islem.getStokFis().getMuhatap().getIsim() + " VERESIYE HESABI", islem.getKurum());


            mahsupFis = new MahsupFis(getSimdikiTarih(), islem.getKurum() , FisKaynak.STOK, "VERESİYE TAKİP KALDIRMA");
            mahsupFis.setKullanici(kullanici);


            //TODO stoğa çevircez burayı
            mahsupFis.setKaynakIslemTip(KaynakIslemTip.ORK_OTOMATIK_IDARI_TAKIBE_GECIS);

//            ModulServisFactroyBean.getIMuhasebeServis().geciciMahsupFisiAc(mahsupFis, kullanici);

            mahsupFis.borcEkle( firma121Hesap, islem.getBorc(), "VERESIYE TAKİP KALDIRMA", getSimdikiTarih() );
            mahsupFis.alacakEkle( firma125Hesap , islem.getBorc(), "VERESIYE TAKİP KALDIRMA", getSimdikiTarih() );

            mahsupFis.setKurum( islem.getKurum() );
            ModulServisFactroyBean.getIMuhasebeServis().kaliciMahsupFisiAc(mahsupFis);


            islem.setIptalFis( mahsupFis );
            islem.setIptalTarihi( getSimdikiTarih() );
            islem.setIptalDurum(SenetIslemIptalDurum.IPTAL_EDILDI);

            kaydet( islem );



    }


}



