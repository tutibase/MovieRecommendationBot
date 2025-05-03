package ru.spbstu.movierecbot.dao.filmapi.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.spbstu.movierecbot.dao.filmapi.CountryApiDao;

import java.util.Arrays;
import java.util.List;

@Repository
public class CountryApiDaoImpl implements CountryApiDao {
    private final List<String> countries;

    @Override
    public Flux<String> getAllCountries() {
        return Flux.fromIterable(countries);
    }

    public CountryApiDaoImpl(ObjectMapper objectMapper) {
        String json = """
                [
                 {
                   "name": "Австралия",
                   "slug": "Avstraliya"
                 },
                 {
                   "name": "Австрия",
                   "slug": "Avstriya"
                 },
                 {
                   "name": "Азербайджан",
                   "slug": "Azerbaydzhan"
                 },
                 {
                   "name": "Албания",
                   "slug": "Albaniya"
                 },
                 {
                   "name": "Алжир",
                   "slug": "Alzhir"
                 },
                 {
                   "name": "Американские Виргинские острова",
                   "slug": "Amerikanskie-Virginskie-ostrova"
                 },
                 {
                   "name": "Американское Самоа",
                   "slug": "Amerikanskoe-Samoa"
                 },
                 {
                   "name": "Ангола",
                   "slug": "Angola"
                 },
                 {
                   "name": "Андорра",
                   "slug": "Andorra"
                 },
                 {
                   "name": "Антарктида",
                   "slug": "Antarktida"
                 },
                 {
                   "name": "Антигуа и Барбуда",
                   "slug": "Antigua-i-Barbuda"
                 },
                 {
                   "name": "Антильские Острова",
                   "slug": "Antilskie-Ostrova"
                 },
                 {
                   "name": "Аргентина",
                   "slug": "Argentina"
                 },
                 {
                   "name": "Армения",
                   "slug": "Armeniya"
                 },
                 {
                   "name": "Аруба",
                   "slug": "Aruba"
                 },
                 {
                   "name": "Афганистан",
                   "slug": "Afganistan"
                 },
                 {
                   "name": "Багамы",
                   "slug": "Bagamy"
                 },
                 {
                   "name": "Бангладеш",
                   "slug": "Bangladesh"
                 },
                 {
                   "name": "Барбадос",
                   "slug": "Barbados"
                 },
                 {
                   "name": "Бахрейн",
                   "slug": "Bahreyn"
                 },
                 {
                   "name": "Беларусь",
                   "slug": "Belarus"
                 },
                 {
                   "name": "Белиз",
                   "slug": "Beliz"
                 },
                 {
                   "name": "Бельгия",
                   "slug": "Belgiya"
                 },
                 {
                   "name": "Бенин",
                   "slug": "Benin"
                 },
                 {
                   "name": "Берег Слоновой кости",
                   "slug": "Bereg-Slonovoy-kosti"
                 },
                 {
                   "name": "Бермуды",
                   "slug": "Bermudy"
                 },
                 {
                   "name": "Бирма",
                   "slug": "Birma"
                 },
                 {
                   "name": "Болгария",
                   "slug": "Bolgariya"
                 },
                 {
                   "name": "Боливия",
                   "slug": "Boliviya"
                 },
                 {
                   "name": "Босния",
                   "slug": "Bosniya"
                 },
                 {
                   "name": "Босния и Герцеговина",
                   "slug": "Bosniya-i-Gercegovina"
                 },
                 {
                   "name": "Ботсвана",
                   "slug": "Botsvana"
                 },
                 {
                   "name": "Бразилия",
                   "slug": "Braziliya"
                 },
                 {
                   "name": "Бруней-Даруссалам",
                   "slug": "Bruney-Darussalam"
                 },
                 {
                   "name": "Буркина-Фасо",
                   "slug": "Burkina-Faso"
                 },
                 {
                   "name": "Бурунди",
                   "slug": "Burundi"
                 },
                 {
                   "name": "Бутан",
                   "slug": "Butan"
                 },
                 {
                   "name": "Вануату",
                   "slug": "Vanuatu"
                 },
                 {
                   "name": "Ватикан",
                   "slug": "Vatikan"
                 },
                 {
                   "name": "Великобритания",
                   "slug": "Velikobritaniya"
                 },
                 {
                   "name": "Венгрия",
                   "slug": "Vengriya"
                 },
                 {
                   "name": "Венесуэла",
                   "slug": "Venesuela"
                 },
                 {
                   "name": "Виргинские Острова",
                   "slug": "Virginskie-Ostrova"
                 },
                 {
                   "name": "Внешние малые острова США",
                   "slug": "Vneshnie-malye-ostrova-SShA"
                 },
                 {
                   "name": "Вьетнам",
                   "slug": "Vetnam"
                 },
                 {
                   "name": "Вьетнам Северный",
                   "slug": "Vetnam-Severnyy"
                 },
                 {
                   "name": "Габон",
                   "slug": "Gabon"
                 },
                 {
                   "name": "Гаити",
                   "slug": "Gaiti"
                 },
                 {
                   "name": "Гайана",
                   "slug": "Gayana"
                 },
                 {
                   "name": "Гамбия",
                   "slug": "Gambiya"
                 },
                 {
                   "name": "Гана",
                   "slug": "Gana"
                 },
                 {
                   "name": "Гваделупа",
                   "slug": "Gvadelupa"
                 },
                 {
                   "name": "Гватемала",
                   "slug": "Gvatemala"
                 },
                 {
                   "name": "Гвинея",
                   "slug": "Gvineya"
                 },
                 {
                   "name": "Гвинея-Бисау",
                   "slug": "Gvineya-Bisau"
                 },
                 {
                   "name": "Германия",
                   "slug": "Germaniya"
                 },
                 {
                   "name": "Германия (ГДР)",
                   "slug": "Germaniya-(GDR)"
                 },
                 {
                   "name": "Германия (ФРГ)",
                   "slug": "Germaniya-(FRG)"
                 },
                 {
                   "name": "Гибралтар",
                   "slug": "Gibraltar"
                 },
                 {
                   "name": "Гондурас",
                   "slug": "Gonduras"
                 },
                 {
                   "name": "Гонконг",
                   "slug": "Gonkong"
                 },
                 {
                   "name": "Гренада",
                   "slug": "Grenada"
                 },
                 {
                   "name": "Гренландия",
                   "slug": "Grenlandiya"
                 },
                 {
                   "name": "Греция",
                   "slug": "Greciya"
                 },
                 {
                   "name": "Грузия",
                   "slug": "Gruziya"
                 },
                 {
                   "name": "Гуам",
                   "slug": "Guam"
                 },
                 {
                   "name": "Дания",
                   "slug": "Daniya"
                 },
                 {
                   "name": "Джибути",
                   "slug": "Dzhibuti"
                 },
                 {
                   "name": "Доминика",
                   "slug": "Dominika"
                 },
                 {
                   "name": "Доминикана",
                   "slug": "Dominikana"
                 },
                 {
                   "name": "Египет",
                   "slug": "Egipet"
                 },
                 {
                   "name": "Заир",
                   "slug": "Zair"
                 },
                 {
                   "name": "Замбия",
                   "slug": "Zambiya"
                 },
                 {
                   "name": "Западная Сахара",
                   "slug": "Zapadnaya-Sahara"
                 },
                 {
                   "name": "Зимбабве",
                   "slug": "Zimbabve"
                 },
                 {
                   "name": "Израиль",
                   "slug": "Izrail"
                 },
                 {
                   "name": "Индия",
                   "slug": "Indiya"
                 },
                 {
                   "name": "Индонезия",
                   "slug": "Indoneziya"
                 },
                 {
                   "name": "Иордания",
                   "slug": "Iordaniya"
                 },
                 {
                   "name": "Ирак",
                   "slug": "Irak"
                 },
                 {
                   "name": "Иран",
                   "slug": "Iran"
                 },
                 {
                   "name": "Ирландия",
                   "slug": "Irlandiya"
                 },
                 {
                   "name": "Исландия",
                   "slug": "Islandiya"
                 },
                 {
                   "name": "Испания",
                   "slug": "Ispaniya"
                 },
                 {
                   "name": "Италия",
                   "slug": "Italiya"
                 },
                 {
                   "name": "Йемен",
                   "slug": "Yemen"
                 },
                 {
                   "name": "Кабо-Верде",
                   "slug": "Kabo-Verde"
                 },
                 {
                   "name": "Казахстан",
                   "slug": "Kazahstan"
                 },
                 {
                   "name": "Каймановы острова",
                   "slug": "Kaymanovy-ostrova"
                 },
                 {
                   "name": "Камбоджа",
                   "slug": "Kambodzha"
                 },
                 {
                   "name": "Камерун",
                   "slug": "Kamerun"
                 },
                 {
                   "name": "Канада",
                   "slug": "Kanada"
                 },
                 {
                   "name": "Катар",
                   "slug": "Katar"
                 },
                 {
                   "name": "Кения",
                   "slug": "Keniya"
                 },
                 {
                   "name": "Кипр",
                   "slug": "Kipr"
                 },
                 {
                   "name": "Кирибати",
                   "slug": "Kiribati"
                 },
                 {
                   "name": "Китай",
                   "slug": "Kitay"
                 },
                 {
                   "name": "Колумбия",
                   "slug": "Kolumbiya"
                 },
                 {
                   "name": "Коморы",
                   "slug": "Komory"
                 },
                 {
                   "name": "Конго",
                   "slug": "Kongo"
                 },
                 {
                   "name": "Конго (ДРК)",
                   "slug": "Kongo-(DRK)"
                 },
                 {
                   "name": "Корея",
                   "slug": "Koreya"
                 },
                 {
                   "name": "Корея Северная",
                   "slug": "Koreya-Severnaya"
                 },
                 {
                   "name": "Корея Южная",
                   "slug": "Koreya-Yuzhnaya"
                 },
                 {
                   "name": "Косово",
                   "slug": "Kosovo"
                 },
                 {
                   "name": "Коста-Рика",
                   "slug": "Kosta-Rika"
                 },
                 {
                   "name": "Кот-д’Ивуар",
                   "slug": "Kot-d'Ivuar"
                 },
                 {
                   "name": "Куба",
                   "slug": "Kuba"
                 },
                 {
                   "name": "Кувейт",
                   "slug": "Kuveyt"
                 },
                 {
                   "name": "Кыргызстан",
                   "slug": "Kyrgyzstan"
                 },
                 {
                   "name": "Лаос",
                   "slug": "Laos"
                 },
                 {
                   "name": "Латвия",
                   "slug": "Latviya"
                 },
                 {
                   "name": "Лесото",
                   "slug": "Lesoto"
                 },
                 {
                   "name": "Либерия",
                   "slug": "Liberiya"
                 },
                 {
                   "name": "Ливан",
                   "slug": "Livan"
                 },
                 {
                   "name": "Ливия",
                   "slug": "Liviya"
                 },
                 {
                   "name": "Литва",
                   "slug": "Litva"
                 },
                 {
                   "name": "Лихтенштейн",
                   "slug": "Lihtenshteyn"
                 },
                 {
                   "name": "Люксембург",
                   "slug": "Lyuksemburg"
                 },
                 {
                   "name": "Маврикий",
                   "slug": "Mavrikiy"
                 },
                 {
                   "name": "Мавритания",
                   "slug": "Mavritaniya"
                 },
                 {
                   "name": "Мадагаскар",
                   "slug": "Madagaskar"
                 },
                 {
                   "name": "Макао",
                   "slug": "Makao"
                 },
                 {
                   "name": "Македония",
                   "slug": "Makedoniya"
                 },
                 {
                   "name": "Малави",
                   "slug": "Malavi"
                 },
                 {
                   "name": "Малайзия",
                   "slug": "Malayziya"
                 },
                 {
                   "name": "Мали",
                   "slug": "Mali"
                 },
                 {
                   "name": "Мальдивы",
                   "slug": "Maldivy"
                 },
                 {
                   "name": "Мальта",
                   "slug": "Malta"
                 },
                 {
                   "name": "Марокко",
                   "slug": "Marokko"
                 },
                 {
                   "name": "Мартиника",
                   "slug": "Martinika"
                 },
                 {
                   "name": "Маршалловы острова",
                   "slug": "Marshallovy-ostrova"
                 },
                 {
                   "name": "Мексика",
                   "slug": "Meksika"
                 },
                 {
                   "name": "Мозамбик",
                   "slug": "Mozambik"
                 },
                 {
                   "name": "Молдова",
                   "slug": "Moldova"
                 },
                 {
                   "name": "Монако",
                   "slug": "Monako"
                 },
                 {
                   "name": "Монголия",
                   "slug": "Mongoliya"
                 },
                 {
                   "name": "Монтсеррат",
                   "slug": "Montserrat"
                 },
                 {
                   "name": "Мьянма",
                   "slug": "Myanma"
                 },
                 {
                   "name": "Намибия",
                   "slug": "Namibiya"
                 },
                 {
                   "name": "Непал",
                   "slug": "Nepal"
                 },
                 {
                   "name": "Нигер",
                   "slug": "Niger"
                 },
                 {
                   "name": "Нигерия",
                   "slug": "Nigeriya"
                 },
                 {
                   "name": "Нидерланды",
                   "slug": "Niderlandy"
                 },
                 {
                   "name": "Никарагуа",
                   "slug": "Nikaragua"
                 },
                 {
                   "name": "Новая Зеландия",
                   "slug": "Novaya-Zelandiya"
                 },
                 {
                   "name": "Новая Каледония",
                   "slug": "Novaya-Kaledoniya"
                 },
                 {
                   "name": "Норвегия",
                   "slug": "Norvegiya"
                 },
                 {
                   "name": "ОАЭ",
                   "slug": "OAE"
                 },
                 {
                   "name": "Оккупированная Палестинская территория",
                   "slug": "Okkupirovannaya-Palestinskaya-territoriya"
                 },
                 {
                   "name": "Оман",
                   "slug": "Oman"
                 },
                 {
                   "name": "Остров Мэн",
                   "slug": "Ostrov-Men"
                 },
                 {
                   "name": "Острова Кука",
                   "slug": "Ostrova-Kuka"
                 },
                 {
                   "name": "Пакистан",
                   "slug": "Pakistan"
                 },
                 {
                   "name": "Палау",
                   "slug": "Palau"
                 },
                 {
                   "name": "Палестина",
                   "slug": "Palestina"
                 },
                 {
                   "name": "Панама",
                   "slug": "Panama"
                 },
                 {
                   "name": "Папуа - Новая Гвинея",
                   "slug": "Papua---Novaya-Gvineya"
                 },
                 {
                   "name": "Парагвай",
                   "slug": "Paragvay"
                 },
                 {
                   "name": "Перу",
                   "slug": "Peru"
                 },
                 {
                   "name": "Польша",
                   "slug": "Polsha"
                 },
                 {
                   "name": "Португалия",
                   "slug": "Portugaliya"
                 },
                 {
                   "name": "Пуэрто Рико",
                   "slug": "Puerto-Riko"
                 },
                 {
                   "name": "Реюньон",
                   "slug": "Reyunon"
                 },
                 {
                   "name": "Российская империя",
                   "slug": "Rossiyskaya-imperiya"
                 },
                 {
                   "name": "Россия",
                   "slug": "Rossiya"
                 },
                 {
                   "name": "Руанда",
                   "slug": "Ruanda"
                 },
                 {
                   "name": "Румыния",
                   "slug": "Rumyniya"
                 },
                 {
                   "name": "СССР",
                   "slug": "SSSR"
                 },
                 {
                   "name": "США",
                   "slug": "SShA"
                 },
                 {
                   "name": "Сальвадор",
                   "slug": "Salvador"
                 },
                 {
                   "name": "Самоа",
                   "slug": "Samoa"
                 },
                 {
                   "name": "Сан-Марино",
                   "slug": "San-Marino"
                 },
                 {
                   "name": "Саудовская Аравия",
                   "slug": "Saudovskaya-Araviya"
                 },
                 {
                   "name": "Свазиленд",
                   "slug": "Svazilend"
                 },
                 {
                   "name": "Северная Македония",
                   "slug": "Severnaya-Makedoniya"
                 },
                 {
                   "name": "Сейшельские острова",
                   "slug": "Seyshelskie-ostrova"
                 },
                 {
                   "name": "Сенегал",
                   "slug": "Senegal"
                 },
                 {
                   "name": "Сент-Винсент и Гренадины",
                   "slug": "Sent-Vinsent-i-Grenadiny"
                 },
                 {
                   "name": "Сент-Китс и Невис",
                   "slug": "Sent-Kits-i-Nevis"
                 },
                 {
                   "name": "Сент-Люсия ",
                   "slug": "Sent-Lyusiya-"
                 },
                 {
                   "name": "Сербия",
                   "slug": "Serbiya"
                 },
                 {
                   "name": "Сербия и Черногория",
                   "slug": "Serbiya-i-Chernogoriya"
                 },
                 {
                   "name": "Сиам",
                   "slug": "Siam"
                 },
                 {
                   "name": "Сингапур",
                   "slug": "Singapur"
                 },
                 {
                   "name": "Сирия",
                   "slug": "Siriya"
                 },
                 {
                   "name": "Словакия",
                   "slug": "Slovakiya"
                 },
                 {
                   "name": "Словения",
                   "slug": "Sloveniya"
                 },
                 {
                   "name": "Соломоновы Острова",
                   "slug": "Solomonovy-Ostrova"
                 },
                 {
                   "name": "Сомали",
                   "slug": "Somali"
                 },
                 {
                   "name": "Судан",
                   "slug": "Sudan"
                 },
                 {
                   "name": "Суринам",
                   "slug": "Surinam"
                 },
                 {
                   "name": "Сьерра-Леоне",
                   "slug": "Serra-Leone"
                 },
                 {
                   "name": "Таджикистан",
                   "slug": "Tadzhikistan"
                 },
                 {
                   "name": "Таиланд",
                   "slug": "Tailand"
                 },
                 {
                   "name": "Тайвань",
                   "slug": "Tayvan"
                 },
                 {
                   "name": "Танзания",
                   "slug": "Tanzaniya"
                 },
                 {
                   "name": "Тимор-Лесте",
                   "slug": "Timor-Leste"
                 },
                 {
                   "name": "Того",
                   "slug": "Togo"
                 },
                 {
                   "name": "Тонга",
                   "slug": "Tonga"
                 },
                 {
                   "name": "Тринидад и Тобаго",
                   "slug": "Trinidad-i-Tobago"
                 },
                 {
                   "name": "Тувалу",
                   "slug": "Tuvalu"
                 },
                 {
                   "name": "Тунис",
                   "slug": "Tunis"
                 },
                 {
                   "name": "Туркменистан",
                   "slug": "Turkmenistan"
                 },
                 {
                   "name": "Турция",
                   "slug": "Turciya"
                 },
                 {
                   "name": "Уганда",
                   "slug": "Uganda"
                 },
                 {
                   "name": "Узбекистан",
                   "slug": "Uzbekistan"
                 },
                 {
                   "name": "Украина",
                   "slug": "Ukraina"
                 },
                 {
                   "name": "Уругвай",
                   "slug": "Urugvay"
                 },
                 {
                   "name": "Фарерские острова",
                   "slug": "Farerskie-ostrova"
                 },
                 {
                   "name": "Федеративные Штаты Микронезии",
                   "slug": "Federativnye-Shtaty-Mikronezii"
                 },
                 {
                   "name": "Фиджи",
                   "slug": "Fidzhi"
                 },
                 {
                   "name": "Филиппины",
                   "slug": "Filippiny"
                 },
                 {
                   "name": "Финляндия",
                   "slug": "Finlyandiya"
                 },
                 {
                   "name": "Фолклендские острова",
                   "slug": "Folklendskie-ostrova"
                 },
                 {
                   "name": "Франция",
                   "slug": "Franciya"
                 },
                 {
                   "name": "Французская Гвиана",
                   "slug": "Francuzskaya-Gviana"
                 },
                 {
                   "name": "Французская Полинезия",
                   "slug": "Francuzskaya-Polineziya"
                 },
                 {
                   "name": "Хорватия",
                   "slug": "Horvatiya"
                 },
                 {
                   "name": "ЦАР",
                   "slug": "CAR"
                 },
                 {
                   "name": "Чад",
                   "slug": "Chad"
                 },
                 {
                   "name": "Черногория",
                   "slug": "Chernogoriya"
                 },
                 {
                   "name": "Чехия",
                   "slug": "Chehiya"
                 },
                 {
                   "name": "Чехословакия",
                   "slug": "Chehoslovakiya"
                 },
                 {
                   "name": "Чили",
                   "slug": "Chili"
                 },
                 {
                   "name": "Швейцария",
                   "slug": "Shveycariya"
                 },
                 {
                   "name": "Швеция",
                   "slug": "Shveciya"
                 },
                 {
                   "name": "Шри-Ланка",
                   "slug": "Shri-Lanka"
                 },
                 {
                   "name": "Эквадор",
                   "slug": "Ekvador"
                 },
                 {
                   "name": "Экваториальная Гвинея",
                   "slug": "Ekvatorialnaya-Gvineya"
                 },
                 {
                   "name": "Эритрея",
                   "slug": "Eritreya"
                 },
                 {
                   "name": "Эсватини",
                   "slug": "Esvatini"
                 },
                 {
                   "name": "Эстония",
                   "slug": "Estoniya"
                 },
                 {
                   "name": "Эфиопия",
                   "slug": "Efiopiya"
                 },
                 {
                   "name": "ЮАР",
                   "slug": "YuAR"
                 },
                 {
                   "name": "Югославия",
                   "slug": "Yugoslaviya"
                 },
                 {
                   "name": "Югославия (ФР)",
                   "slug": "Yugoslaviya-(FR)"
                 },
                 {
                   "name": "Ямайка",
                   "slug": "Yamayka"
                 },
                 {
                   "name": "Япония",
                   "slug": "Yaponiya"
                 }
                ]""";
        try {
            CountryApiDaoImpl.Country[] genres = objectMapper.readValue(json, CountryApiDaoImpl.Country[].class);
            this.countries = Arrays.stream(genres)
                    .map(CountryApiDaoImpl.Country::name)
                    .toList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка парсинга стран", e);
        }
    }

    private record Country(String name, String slug) {}
}
