package com.finere.scan_and_go_api.service;

import com.finere.scan_and_go_api.dto.qr.LabelItem;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** Renders a printable A4 label sheet (3x8 grid) with a QR code, product name, SKU and lot number per cell. */
@Service
public class PdfLabelSheetService {

    private static final int COLUMNS = 3;
    private static final int ROWS = 8;
    private static final int CELLS_PER_PAGE = COLUMNS * ROWS;
    private static final float CELL_PADDING = 6f;

    public byte[] renderA4Sheet(List<LabelItem> items) {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font detailFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            for (int pageStart = 0; pageStart < items.size(); pageStart += CELLS_PER_PAGE) {
                PdfPTable table = new PdfPTable(COLUMNS);
                table.setWidthPercentage(100);

                int pageEnd = Math.min(pageStart + CELLS_PER_PAGE, items.size());
                for (int i = pageStart; i < pageEnd; i++) {
                    table.addCell(buildLabelCell(items.get(i), nameFont, detailFont));
                }

                int remainder = (pageEnd - pageStart) % COLUMNS;
                if (remainder != 0) {
                    for (int i = 0; i < COLUMNS - remainder; i++) {
                        table.addCell(emptyCell());
                    }
                }

                document.add(table);
                if (pageEnd < items.size()) {
                    document.newPage();
                }
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new UncheckedIOException("Unable to render label sheet PDF", wrapAsIOException(e));
        }
    }

    private PdfPCell buildLabelCell(LabelItem item, Font nameFont, Font detailFont) throws IOException, DocumentException {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);

        Image qrImage = Image.getInstance(item.qrPng());
        qrImage.scaleToFit(90, 90);
        qrImage.setAlignment(Element.ALIGN_CENTER);

        PdfPCell imageCell = new PdfPCell(qrImage, false);
        imageCell.setBorder(0);
        imageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        inner.addCell(imageCell);

        inner.addCell(textCell(item.productName(), nameFont));
        inner.addCell(textCell("SKU: " + item.sku(), detailFont));
        if (item.lotNumber() != null) {
            inner.addCell(textCell("Lot: " + item.lotNumber(), detailFont));
        }

        PdfPCell outer = new PdfPCell(inner);
        outer.setPadding(CELL_PADDING);
        return outer;
    }

    private PdfPCell textCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setBorder(0);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        return cell;
    }

    private IOException wrapAsIOException(Exception e) {
        return e instanceof IOException io ? io : new IOException(e);
    }
}
