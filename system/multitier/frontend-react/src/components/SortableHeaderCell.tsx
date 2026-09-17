import { flexRender, type Header } from '@tanstack/react-table';

const ARIA_SORT = { asc: 'ascending', desc: 'descending' } as const;

/**
 * Table header cell for a TanStack column. A sortable column renders its label as a button,
 * so sorting works from the keyboard, and announces the current order through aria-sort.
 */
export function SortableHeaderCell<TData>({ header }: Readonly<{ header: Header<TData, unknown> }>) {
  const label = flexRender(header.column.columnDef.header, header.getContext());

  if (!header.column.getCanSort()) {
    return <th>{label}</th>;
  }

  const sorted = header.column.getIsSorted();

  return (
    <th aria-sort={sorted ? ARIA_SORT[sorted] : 'none'}>
      <button
        type="button"
        className="btn btn-link p-0 fw-bold text-reset text-decoration-none d-flex align-items-center"
        onClick={header.column.getToggleSortingHandler()}
      >
        {label}
        {sorted && <span className="ms-1" aria-hidden="true">{sorted === 'asc' ? '↑' : '↓'}</span>}
      </button>
    </th>
  );
}
