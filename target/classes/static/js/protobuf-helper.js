export function decodePriceUpdate(buffer) {
  const result = { symbol: "", price: "" };
  let offset = 0;

  while (offset < buffer.length) {
    const tag = buffer[offset++];

    if (tag === 0x0a) {
      const length = buffer[offset++];
      const symbolBytes = buffer.slice(offset, offset + length);
      result.symbol = new TextDecoder("utf-8").decode(symbolBytes);
      offset += length;
    } else if (tag === 0x12) {
      const length = buffer[offset++];
      const priceBytes = buffer.slice(offset, offset + length);
      result.price = new TextDecoder("utf-8").decode(priceBytes);
      offset += length;
    } else {
      break;
    }
  }

  return result;
}
