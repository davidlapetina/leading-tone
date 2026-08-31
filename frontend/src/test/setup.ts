import '@testing-library/jest-dom/vitest'
import { vi } from 'vitest'

// jsdom has no layout engine, so it has no scrollIntoView. Nothing under test depends on
// the scroll actually happening.
Element.prototype.scrollIntoView = vi.fn()
