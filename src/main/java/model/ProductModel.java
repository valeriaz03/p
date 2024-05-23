package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

public class ProductModel {

    private static final String TABLE_NAME = "Prodotto";

    public synchronized void doSave(ProductBean product) throws SQLException {
        String insertSQL = "INSERT INTO Prodotto (nome, descrizione, prezzo, speseSpedizione, emailVenditore, tag, nomeTipologia, model, dataAnnuncio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, current_date())";

        try (Connection connection = DriverManagerConnectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {

            preparedStatement.setString(1, product.getNome());
            preparedStatement.setString(2, product.getDescrizione());
            preparedStatement.setDouble(3, product.getPrezzo());
            preparedStatement.setDouble(4, product.getSpedizione());
            preparedStatement.setString(5, product.getEmail());
            preparedStatement.setString(6, product.getTag());
            preparedStatement.setString(7, product.getTipologia());
            preparedStatement.setString(8, product.getImmagine());

            preparedStatement.executeUpdate();
            connection.commit();
        }
    }

    public synchronized ProductBean doRetrieveByKey(int code) throws SQLException {
        String selectSQL = "SELECT * FROM " + TABLE_NAME + " WHERE codice = ? AND deleted = false";
        ProductBean bean = new ProductBean();

        try (Connection connection = DriverManagerConnectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {

            preparedStatement.setInt(1, code);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    bean.setCodice(rs.getInt("codice"));
                    bean.setNome(rs.getString("nome"));
                    bean.setDescrizione(rs.getString("descrizione"));
                    bean.setPrezzo(rs.getDouble("prezzo"));
                    bean.setSpedizione(rs.getDouble("speseSpedizione"));
                    bean.setEmail(rs.getString("emailVenditore"));
                    bean.setTag(rs.getString("tag"));
                    bean.setTipologia(rs.getString("nomeTipologia"));
                    bean.setData(rs.getDate("dataAnnuncio"));
                    bean.setImmagine(rs.getString("model"));
                }
            }
        }
        return bean;
    }

    public synchronized boolean doDelete(int code) throws SQLException {
        String deleteSQL = "UPDATE " + TABLE_NAME + " SET deleted = true WHERE codice = ?";
        int result;

        try (Connection connection = DriverManagerConnectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(deleteSQL)) {

            preparedStatement.setInt(1, code);
            result = preparedStatement.executeUpdate();
            connection.commit();
        }
        return (result != 0);
    }

    public synchronized Collection<ProductBean> doRetrieveAll(String where) throws SQLException {
        String selectSQL = "SELECT * FROM " + TABLE_NAME + " WHERE deleted = false AND nomeTipologia = ?";
        String sql2 = "SELECT AVG(votazione) FROM Recensione WHERE codiceProdotto = ?";
        Collection<ProductBean> products = new LinkedList<>();

        try (Connection connection = DriverManagerConnectionPool.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {

            preparedStatement.setString(1, where);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    ProductBean bean = new ProductBean();

                    int codiceProdotto = rs.getInt("codice");
                    bean.setCodice(codiceProdotto);
                    bean.setNome(rs.getString("nome"));
                    bean.setDescrizione(rs.getString("descrizione"));
                    bean.setPrezzo(rs.getDouble("prezzo"));
                    bean.setSpedizione(rs.getDouble("speseSpedizione"));
                    bean.setEmail(rs.getString("emailVenditore"));
                    bean.setTag(rs.getString("tag"));
                    bean.setTipologia(rs.getString("nomeTipologia"));
                    bean.setData(rs.getDate("dataAnnuncio"));
                    bean.setImmagine(rs.getString("model"));

                    try (Connection connection2 = DriverManagerConnectionPool.getConnection();
                         PreparedStatement preparedStatement2 = connection2.prepareStatement(sql2)) {

                        preparedStatement2.setInt(1, codiceProdotto);

                        try (ResultSet rs2 = preparedStatement2.executeQuery()) {
                            if (rs2.next()) {
                                bean.setVotazione(rs2.getDouble(1));
                            }
                        }
                    }
                    products.add(bean);
                }
            }
        }
        return products;
    }

    public synchronized Collection<ProductBean> deleteProduct(int codiceProdotto, Collection<ProductBean> lista) {
        String sql = "UPDATE Prodotto SET deleted = ? WHERE codice = ?";

        try (Connection con = DriverManagerConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, true);
            ps.setInt(2, codiceProdotto);

            ps.executeUpdate();
            con.commit();

            lista.removeIf(a -> a.getCodice() == codiceProdotto);

        } catch (SQLException e) {
            // Log the exception
        }
        return lista;
    }

    public synchronized void updateProduct(ProductBean bean) {
        String sql = "UPDATE Prodotto SET nome = ?, descrizione = ?, prezzo = ?, speseSpedizione = ?, tag = ?, nomeTipologia = ? WHERE codice = ?";

        try (Connection con = DriverManagerConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, bean.getNome());
            ps.setString(2, bean.getDescrizione());
            ps.setDouble(3, bean.getPrezzo());
            ps.setDouble(4, bean.getSpedizione());
            ps.setString(5, bean.getTag());
            ps.setString(6, bean.getTipologia());
            ps.setInt(7, bean.getCodice());

            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            // Log the exception
        }
    }

    public synchronized RecensioneBean getRecensione(int codiceProdotto, String email) {
        String sql2 = "SELECT votazione, testo FROM Recensione WHERE codiceProdotto = ? AND emailCliente = ?";
        RecensioneBean bean = new RecensioneBean();

        try (Connection con = DriverManagerConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(sql2)) {

            ps.setInt(1, codiceProdotto);
            ps.setString(2, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bean.setVoto(rs.getInt(1));
                    bean.setTesto(rs.getString(2));
                }
            }
        } catch (SQLException e) {
            // Log the exception
            return null;
        }
        return bean;
    }

    public synchronized String getRandomCode() {
        String sql2 = "SELECT nome, nomeTipologia FROM Prodotto ORDER BY RAND() LIMIT 1";
        String riprova = "Errore: riprova";

        try (Connection con = DriverManagerConnectionPool.getConnection();
             PreparedStatement ps = con.prepareStatement(sql2)) {

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nome = rs.getString(1);
                    String tipologia = rs.getString(2);
                    return "Dai un'occhiata a " + nome + " in " + tipologia;
                }
            }
        } catch (SQLException e) {
            // Log the exception
        }
        return riprova;
    }
}

